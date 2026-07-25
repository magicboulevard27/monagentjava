package com.monagent.collection.scheduling;

import com.monagent.api.service.MonitoredServiceService;
import com.monagent.domain.MonitoredService;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledCollectorCoordinator {

    private final MonitoredServiceService monitoredServiceService;
    private final CollectorWorkDispatcher dispatcher;
    private final CollectorLeaderElectionService leaderElectionService;
    private final com.monagent.config.AsyncProcessingProperties properties;

    public ScheduledCollectorCoordinator(
            MonitoredServiceService monitoredServiceService,
            CollectorWorkDispatcher dispatcher,
            CollectorLeaderElectionService leaderElectionService,
            com.monagent.config.AsyncProcessingProperties properties) {
        this.monitoredServiceService = monitoredServiceService;
        this.dispatcher = dispatcher;
        this.leaderElectionService = leaderElectionService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${monagent.async.dispatch-interval-ms:60000}")
    public void scheduleCollectors() {
        if (!leaderElectionService.tryAcquire(properties.leaderLeaseDuration())) {
            return;
        }
        for (MonitoredService service : monitoredServiceService.listEnabled()) {
            dispatchForService(service);
        }
    }

    public void dispatchForService(MonitoredService service) {
        List.of(
                new CollectorJob(CollectorJobType.HEALTH, service.serviceId().toString(), idempotencyKey(service, CollectorJobType.HEALTH)),
                new CollectorJob(CollectorJobType.PROMETHEUS, service.serviceId().toString(), idempotencyKey(service, CollectorJobType.PROMETHEUS)),
                new CollectorJob(CollectorJobType.LOGS, service.serviceId().toString(), idempotencyKey(service, CollectorJobType.LOGS)),
                new CollectorJob(CollectorJobType.TRACES, service.serviceId().toString(), idempotencyKey(service, CollectorJobType.TRACES)),
                new CollectorJob(CollectorJobType.KUBERNETES, service.serviceId().toString(), idempotencyKey(service, CollectorJobType.KUBERNETES)))
                .forEach(dispatcher::dispatch);
    }

    private String idempotencyKey(MonitoredService service, CollectorJobType type) {
        return "collector:" + type.name().toLowerCase() + ":" + service.serviceId();
    }
}

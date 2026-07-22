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

    public ScheduledCollectorCoordinator(
            MonitoredServiceService monitoredServiceService,
            CollectorWorkDispatcher dispatcher) {
        this.monitoredServiceService = monitoredServiceService;
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelayString = "${monagent.async.dispatch-interval-ms:60000}")
    public void scheduleCollectors() {
        for (MonitoredService service : monitoredServiceService.list()) {
            if (!service.enabled()) {
                continue;
            }
            dispatchForService(service);
        }
    }

    public void dispatchForService(MonitoredService service) {
        List.of(
                new CollectorJob(CollectorJobType.HEALTH, service.serviceId().toString()),
                new CollectorJob(CollectorJobType.PROMETHEUS, service.serviceId().toString()),
                new CollectorJob(CollectorJobType.LOGS, service.serviceId().toString()),
                new CollectorJob(CollectorJobType.TRACES, service.serviceId().toString()),
                new CollectorJob(CollectorJobType.KUBERNETES, service.serviceId().toString()))
                .forEach(dispatcher::dispatch);
    }
}

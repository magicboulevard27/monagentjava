package com.monagent.collection.health;

import com.monagent.collection.MonitoringSignalPersistenceService;
import com.monagent.collection.model.HealthSourceSignal;
import com.monagent.collection.model.NormalizedSignal;
import com.monagent.collection.model.SignalSeverity;
import com.monagent.collection.model.SignalStatus;
import com.monagent.api.service.MonitoredServiceService;
import com.monagent.domain.MonitoredService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class HealthCollectorService {

    private static final Logger log = LoggerFactory.getLogger(HealthCollectorService.class);

    private final MonitoredServiceService monitoredServiceService;
    private final HealthCollectorClient client;
    private final HealthCollectorProperties properties;
    private final MonitoringSignalPersistenceService persistenceService;

    public HealthCollectorService(
            MonitoredServiceService monitoredServiceService,
            HealthCollectorClient client,
            HealthCollectorProperties properties,
            MonitoringSignalPersistenceService persistenceService) {
        this.monitoredServiceService = monitoredServiceService;
        this.client = client;
        this.properties = properties;
        this.persistenceService = persistenceService;
    }

    @Scheduled(fixedDelayString = "${monagent.collectors.health.interval-seconds:60}000")
    public void collect() {
        List<NormalizedSignal> batch = new ArrayList<>();
        for (MonitoredService service : monitoredServiceService.list()) {
            if (!service.enabled()) {
                continue;
            }
            try {
                HealthCollectionResult result = collect(service);
                batch.add(result.signal());
            } catch (RuntimeException ex) {
                // Continue the batch so one bad endpoint does not stop unrelated services.
            }
        }
        persistenceService.saveAll(batch);
    }

    public HealthCollectionResult collect(MonitoredService service) {
        Map<String, Object> payload = client.fetchHealth(service.healthUrl(), properties.timeout());
        String healthState = stringify(payload.get("status"));
        boolean dependencyIssue = hasDependencyIssue(payload);
        SignalStatus status = HealthStatusMapper.mapStatus(healthState, dependencyIssue);
        SignalSeverity severity = HealthStatusMapper.mapSeverity(status);

        HealthSourceSignal source = new HealthSourceSignal(
                service.serviceId(),
                service.serviceName(),
                service.environment(),
                Instant.now(),
                healthState,
                dependencyIssue,
                stringify(payload));

        NormalizedSignal normalizedSignal = new com.monagent.collection.SignalNormalizationService().fromHealth(source);
        persistenceService.save(normalizedSignal);
        return new HealthCollectionResult(normalizedSignal, true);
    }

    private boolean hasDependencyIssue(Map<String, Object> payload) {
        Object components = payload.get("components");
        return components != null && stringify(components).toLowerCase(Locale.ROOT).contains("down");
    }

    private String stringify(Object value) {
        return value == null ? "" : value.toString();
    }
}

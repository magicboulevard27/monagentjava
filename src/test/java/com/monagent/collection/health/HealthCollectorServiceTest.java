package com.monagent.collection.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.monagent.api.service.MonitoredServiceService;
import com.monagent.collection.MonitoringSignalPersistenceService;
import com.monagent.collection.model.NormalizedSignal;
import com.monagent.collection.model.SignalSeverity;
import com.monagent.collection.model.SignalStatus;
import com.monagent.domain.MonitoredService;
import com.monagent.web.SelfObservabilityMetrics;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HealthCollectorServiceTest {

    private final MonitoredServiceService monitoredServiceService = Mockito.mock(MonitoredServiceService.class);
    private final HealthCollectorClient client = Mockito.mock(HealthCollectorClient.class);
    private final MonitoringSignalPersistenceService persistenceService = Mockito.mock(MonitoringSignalPersistenceService.class);
    private final SelfObservabilityMetrics metrics = new SelfObservabilityMetrics(new SimpleMeterRegistry());

    @Test
    void collectOptionallyFetchesInfoPayload() {
        var properties = new HealthCollectorProperties(Duration.ofSeconds(5), 60, 1, true);
        var service = new HealthCollectorService(monitoredServiceService, client, properties, persistenceService, metrics);
        var monitoredService = new MonitoredService(UUID.randomUUID(), "orders", "prod", "team",
                "http://localhost/actuator/health", null, null, null, null, null, List.of(), true);
        when(client.fetchHealth("http://localhost/actuator/health", Duration.ofSeconds(5)))
                .thenReturn(Map.of("status", "UP"));
        when(client.fetchInfo("http://localhost/actuator/health", Duration.ofSeconds(5)))
                .thenReturn(Map.of("build", Map.of("version", "1.0.0")));

        NormalizedSignal signal = service.collect(monitoredService).signal();

        assertThat(signal.rawReference()).contains("info=");
        verify(client).fetchInfo("http://localhost/actuator/health", Duration.ofSeconds(5));
        verify(persistenceService).save(signal);
    }

    @Test
    void collectMapsHealthyDegradedAndMalformedResponses() {
        var properties = new HealthCollectorProperties(Duration.ofSeconds(5), 60, 1, false);
        var service = new HealthCollectorService(monitoredServiceService, client, properties, persistenceService, metrics);
        var monitoredService = new MonitoredService(UUID.randomUUID(), "orders", "prod", "team",
                "http://localhost/actuator/health", null, null, null, null, null, List.of(), true);

        when(client.fetchHealth("http://localhost/actuator/health", Duration.ofSeconds(5)))
                .thenReturn(Map.of("status", "UP"))
                .thenReturn(Map.of("status", "DOWN", "components", Map.of("db", Map.of("status", "DOWN"))))
                .thenReturn(Map.of());

        var healthy = service.collect(monitoredService).signal();
        var degraded = service.collect(monitoredService).signal();
        var malformed = service.collect(monitoredService).signal();

        assertThat(healthy.status()).isEqualTo(SignalStatus.UP);
        assertThat(healthy.severity()).isEqualTo(SignalSeverity.NONE);
        assertThat(degraded.status()).isEqualTo(SignalStatus.DOWN);
        assertThat(degraded.severity()).isEqualTo(SignalSeverity.CRITICAL);
        assertThat(malformed.status()).isEqualTo(SignalStatus.UNKNOWN);
        assertThat(malformed.severity()).isEqualTo(SignalSeverity.NONE);
    }

    @Test
    void collectContinuesWhenOneServiceTimesOutOrIsUnavailable() {
        var properties = new HealthCollectorProperties(Duration.ofSeconds(5), 60, 1, false);
        var service = new HealthCollectorService(monitoredServiceService, client, properties, persistenceService, metrics);
        var healthyService = new MonitoredService(UUID.randomUUID(), "orders", "prod", "team",
                "http://localhost/actuator/health", null, null, null, null, null, List.of(), true);
        var brokenService = new MonitoredService(UUID.randomUUID(), "billing", "prod", "team",
                "http://localhost/broken/health", null, null, null, null, null, List.of(), true);

        when(monitoredServiceService.listEnabled()).thenReturn(List.of(healthyService, brokenService));
        when(client.fetchHealth("http://localhost/actuator/health", Duration.ofSeconds(5)))
                .thenReturn(Map.of("status", "UP"));
        when(client.fetchHealth("http://localhost/broken/health", Duration.ofSeconds(5)))
                .thenThrow(new RuntimeException("timeout"));

        service.collect();

        verify(persistenceService).saveAll(any());
        verify(client).fetchHealth("http://localhost/actuator/health", Duration.ofSeconds(5));
        verify(client).fetchHealth("http://localhost/broken/health", Duration.ofSeconds(5));
    }
}

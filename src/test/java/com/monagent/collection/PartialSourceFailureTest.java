package com.monagent.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.monagent.api.service.MonitoredServiceService;
import com.monagent.collection.health.HealthCollectorClient;
import com.monagent.collection.health.HealthCollectorProperties;
import com.monagent.collection.health.HealthCollectorService;
import com.monagent.collection.model.NormalizedSignal;
import com.monagent.collection.prometheus.PrometheusCollectorProperties;
import com.monagent.collection.prometheus.PrometheusMetricsCollectorService;
import com.monagent.collection.prometheus.PrometheusQueryClient;
import com.monagent.domain.MonitoredService;
import com.monagent.web.SelfObservabilityMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PartialSourceFailureTest {

    @Test
    void continuesCollectingOtherSourcesWhenOneCollectorFails() {
        SelfObservabilityMetrics metrics = new SelfObservabilityMetrics(new SimpleMeterRegistry());
        MonitoringSignalPersistenceService persistenceService = mock(MonitoringSignalPersistenceService.class);
        when(persistenceService.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        HealthCollectorClient healthClient = mock(HealthCollectorClient.class);
        when(healthClient.fetchHealth("http://localhost/actuator/health", Duration.ofSeconds(5)))
                .thenThrow(new RuntimeException("health unavailable"))
                .thenReturn(Map.of("status", "UP"));
        HealthCollectorService healthCollectorService = new HealthCollectorService(
                mock(MonitoredServiceService.class),
                healthClient,
                new HealthCollectorProperties(Duration.ofSeconds(5), 30, 1, false),
                persistenceService,
                metrics);

        PrometheusQueryClient prometheusQueryClient = mock(PrometheusQueryClient.class);
        when(prometheusQueryClient.query("/api/v1/query", "cpu", Duration.ofSeconds(5)))
                .thenReturn(Map.of("status", "success", "data", Map.of("resultType", "vector", "result", List.of(
                        Map.of("value", Map.of("0", "2026-07-22T12:00:00Z", "1", "42.5"))))));
        when(prometheusQueryClient.query("/api/v1/query", "memory", Duration.ofSeconds(5)))
                .thenThrow(new RuntimeException("prometheus unavailable"));
        PrometheusMetricsCollectorService prometheusCollectorService = new PrometheusMetricsCollectorService(
                mock(MonitoredServiceService.class),
                new PrometheusCollectorProperties(Duration.ofSeconds(5), URI.create("https://prometheus.internal:9090"),
                        null, null, null, List.of(
                                new PrometheusCollectorProperties.MetricQuery("cpu", "cpu", "cores"),
                                new PrometheusCollectorProperties.MetricQuery("memory", "memory", "bytes"))),
                prometheusQueryClient,
                persistenceService,
                new SignalNormalizationService());

        MonitoredService monitoredService = new MonitoredService(UUID.randomUUID(), "orders", "prod", "team",
                "http://localhost/actuator/health", "job", null, null, null, null, List.of(), true);

        assertThatThrownBy(() -> healthCollectorService.collect(monitoredService)).isInstanceOf(RuntimeException.class);
        List<NormalizedSignal> metricsSignals = prometheusCollectorService.collect(monitoredService);

        assertThat(metricsSignals).hasSize(1);
    }
}

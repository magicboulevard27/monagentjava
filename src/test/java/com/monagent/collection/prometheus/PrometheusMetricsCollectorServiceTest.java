package com.monagent.collection.prometheus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.monagent.api.service.MonitoredServiceService;
import com.monagent.collection.MonitoringSignalPersistenceService;
import com.monagent.collection.SignalNormalizationService;
import com.monagent.collection.model.NormalizedSignal;
import com.monagent.domain.MonitoredService;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PrometheusMetricsCollectorServiceTest {

    private final MonitoredServiceService monitoredServiceService = mock(MonitoredServiceService.class);
    private final PrometheusQueryClient client = mock(PrometheusQueryClient.class);
    private final MonitoringSignalPersistenceService persistenceService = mock(MonitoringSignalPersistenceService.class);
    private final SignalNormalizationService normalizationService = new SignalNormalizationService();

    @Test
    void skipsMissingAndFailedSeriesWithoutFailingBatch() {
        var properties = new PrometheusCollectorProperties(
                Duration.ofSeconds(5),
                URI.create("https://prometheus.internal:9090"),
                null,
                null,
                null,
                List.of(
                        new PrometheusCollectorProperties.MetricQuery("cpu", "cpu_query", "cores"),
                        new PrometheusCollectorProperties.MetricQuery("memory", "memory_query", "bytes")));
        var service = new PrometheusMetricsCollectorService(monitoredServiceService, properties, client, persistenceService, normalizationService);
        var monitoredService = new MonitoredService(UUID.randomUUID(), "orders", "prod", "team",
                "https://orders.internal/actuator/health", "job", null, null, null, null, List.of(), true);

        when(client.query("/api/v1/query", "cpu_query", Duration.ofSeconds(5)))
                .thenReturn(Map.of("status", "success", "data", Map.of("resultType", "vector", "result", List.of(
                        Map.of("value", Map.of("0", "2026-07-22T12:00:00Z", "1", "42.5"))))));
        when(client.query("/api/v1/query", "memory_query", Duration.ofSeconds(5)))
                .thenReturn(Map.of("status", "error", "errorType", "bad_data", "data", Map.of("resultType", "vector", "result", List.of())));
        when(monitoredServiceService.listEnabled()).thenReturn(List.of(monitoredService));
        when(persistenceService.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<NormalizedSignal> signals = service.collect(monitoredService);

        assertThat(signals).hasSize(1);
        assertThat(signals.getFirst().signalName()).isEqualTo("cpu");
    }

    @Test
    void continuesWhenQueryThrowsAndReturnsPartialBatch() {
        var properties = new PrometheusCollectorProperties(
                Duration.ofSeconds(5),
                URI.create("https://prometheus.internal:9090"),
                null,
                null,
                null,
                List.of(new PrometheusCollectorProperties.MetricQuery("cpu", "cpu_query", "cores")));
        var service = new PrometheusMetricsCollectorService(monitoredServiceService, properties, client, persistenceService, normalizationService);
        var monitoredService = new MonitoredService(UUID.randomUUID(), "orders", "prod", "team",
                "https://orders.internal/actuator/health", "job", null, null, null, null, List.of(), true);

        when(client.query("/api/v1/query", "cpu_query", Duration.ofSeconds(5)))
                .thenThrow(new RuntimeException("timeout"));

        List<NormalizedSignal> signals = service.collect(monitoredService);

        assertThat(signals).isEmpty();
    }
}

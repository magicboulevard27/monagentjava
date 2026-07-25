package com.monagent.collection.prometheus;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PrometheusMetricMapperTest {

    @Test
    void parsesSingleSample() {
        Map<String, Object> response = Map.of(
                "data", Map.of(
                        "result", java.util.List.of(
                                Map.of("value", Map.of("0", "2026-07-22T12:00:00Z", "1", "42.5")))));

        var samples = PrometheusMetricMapper.parseInstantVector("order-service", "prod", response, "cpu", "cores");

        assertThat(samples).hasSize(1);
        assertThat(samples.getFirst().metricName()).isEqualTo("cpu");
        assertThat(samples.getFirst().value()).isEqualTo(42.5);
        assertThat(samples.getFirst().observedAt()).isEqualTo(Instant.parse("2026-07-22T12:00:00Z"));
    }

    @Test
    void normalizesMetricLabelsUnitsValuesAndTimestamps() {
        Map<String, Object> response = Map.of(
                "data", Map.of(
                        "result", java.util.List.of(
                                Map.of(
                                        "metric", Map.of("instance", "Orders-01", "pod", "orders-abc"),
                                        "value", Map.of("0", "bad-timestamp", "1", "NaN")))));

        var samples = PrometheusMetricMapper.parseInstantVector("order-service", "prod", response, "Request Rate", "Milliseconds");
        var source = PrometheusMetricMapper.toSourceSignal(samples.getFirst());

        assertThat(samples.getFirst().unit()).isEqualTo("ms");
        assertThat(samples.getFirst().observedAt()).isEqualTo(Instant.EPOCH);
        assertThat(source.metricName()).isEqualTo("request.rate");
        assertThat(source.unit()).isEqualTo("ms");
        assertThat(source.value()).isEqualTo(0.0);
        assertThat(source.rawReference()).contains("observedAt=1970-01-01T00:00:00Z");
        assertThat(samples.getFirst().rawReference()).contains("instance=Orders-01");
    }

    @Test
    void returnsEmptyForMissingPartialOrErrorResponses() {
        assertThat(PrometheusMetricMapper.parseInstantVector("order-service", "prod", Map.of(), "cpu", "cores")).isEmpty();
        assertThat(PrometheusMetricMapper.parseInstantVector("order-service", "prod",
                Map.of("status", "error", "errorType", "timeout"), "cpu", "cores")).isEmpty();
        assertThat(PrometheusMetricMapper.parseInstantVector("order-service", "prod",
                Map.of("status", "success", "data", Map.of("resultType", "matrix", "result", List.of())), "cpu", "cores")).isEmpty();
    }
}

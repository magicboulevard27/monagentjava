package com.monagent.collection.prometheus;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
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
}

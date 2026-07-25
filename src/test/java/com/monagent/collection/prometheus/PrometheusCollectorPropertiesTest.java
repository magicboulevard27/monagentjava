package com.monagent.collection.prometheus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrometheusCollectorPropertiesTest {

    @Test
    void rejectsAuthOverPlainHttp() {
        assertThatThrownBy(() -> new PrometheusCollectorProperties(
                Duration.ofSeconds(10),
                URI.create("http://prometheus.internal:9090"),
                "token",
                null,
                null,
                List.of(new PrometheusCollectorProperties.MetricQuery("cpu", "up", "cores"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("https baseUrl");
    }
}

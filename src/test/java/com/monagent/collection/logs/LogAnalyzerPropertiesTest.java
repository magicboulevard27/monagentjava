package com.monagent.collection.logs;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class LogAnalyzerPropertiesTest {

    @Test
    void rejectsAuthOverPlainHttp() {
        assertThatThrownBy(() -> new LogAnalyzerProperties(
                URI.create("http://opensearch.internal:9200"),
                "/_search",
                Duration.ofSeconds(10),
                10,
                "token",
                null,
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("https baseUrl");
    }
}

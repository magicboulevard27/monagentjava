package com.monagent.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SelfObservabilityMetricsTest {

    @Test
    void recordsSourceQueryAndAiReasoningLatencyMetrics() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        SelfObservabilityMetrics metrics = new SelfObservabilityMetrics(meterRegistry);

        String sourceResult = metrics.timeSourceQuery("logs", () -> "ok");
        String aiResult = metrics.timeAiReasoning("ollama", () -> "done");

        assertThat(sourceResult).isEqualTo("ok");
        assertThat(aiResult).isEqualTo("done");
        assertThat(meterRegistry.find("monagent.source.query.latency").timer()).isNotNull();
        assertThat(meterRegistry.find("monagent.ai.reasoning.latency").timer()).isNotNull();
        assertThat(meterRegistry.find("monagent.source.query.latency").timer().getId().getTag("stage")).isEqualTo("logs");
        assertThat(meterRegistry.find("monagent.ai.reasoning.latency").timer().getId().getTag("stage")).isEqualTo("ollama");
    }
}

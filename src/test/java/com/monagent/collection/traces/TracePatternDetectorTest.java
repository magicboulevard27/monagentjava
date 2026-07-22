package com.monagent.collection.traces;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TracePatternDetectorTest {

    private final TracePatternDetector detector = new TracePatternDetector();

    @Test
    void detectsLatencyAndDownstreamFailure() {
        assertThat(detector.detect("slow request path", 100, "ok", null)).isEqualTo("slow request path");
        assertThat(detector.detect("anything", 2501, "ok", null)).isEqualTo("high latency span");
        assertThat(detector.detect("anything", 100, "error", null)).isEqualTo("failed downstream dependency");
    }
}

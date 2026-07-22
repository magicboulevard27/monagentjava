package com.monagent.collection.logs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogPatternDetectorTest {

    private final LogPatternDetector detector = new LogPatternDetector();

    @Test
    void detectsOutOfMemory() {
        assertThat(detector.detect("java.lang.OutOfMemoryError: Java heap space")).isEqualTo("outofmemoryerror");
    }
}

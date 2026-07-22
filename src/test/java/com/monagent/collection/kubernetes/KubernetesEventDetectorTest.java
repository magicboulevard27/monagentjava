package com.monagent.collection.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KubernetesEventDetectorTest {

    private final KubernetesEventDetector detector = new KubernetesEventDetector();

    @Test
    void detectsDeploymentAndFailureEvents() {
        assertThat(detector.detect("Warning", "Pod restarted")).isEqualTo("warning");
        assertThat(detector.detect("Failed", "Pod scheduling failed")).isEqualTo("failed");
        assertThat(detector.detect("Scheduled", "deployment rollout complete")).isEqualTo("deployment");
        assertThat(detector.detect("CrashLoopBackOff", "Back-off restarting failed container")).isEqualTo("crashloopbackoff");
    }
}

package com.monagent.collection.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class KubernetesCollectorPropertiesTest {

    @Test
    void resolvesLocalBaseUrlByDefault() {
        KubernetesCollectorProperties properties = new KubernetesCollectorProperties(
                "/apis/monitoring.monagent.io/v1/events",
                Duration.ofSeconds(5),
                15,
                false,
                null,
                null,
                null);

        assertThat(properties.resolvedBaseUrl()).isEqualTo(URI.create("http://localhost:8001"));
    }

    @Test
    void resolvesInClusterBaseUrlWhenConfigured() {
        KubernetesCollectorProperties properties = new KubernetesCollectorProperties(
                "/apis/monitoring.monagent.io/v1/events",
                Duration.ofSeconds(5),
                15,
                true,
                URI.create("https://kubernetes.default.svc"),
                "token",
                "/var/run/secrets/kubernetes.io/serviceaccount/token");

        assertThat(properties.resolvedBaseUrl()).isEqualTo(URI.create("https://kubernetes.default.svc"));
    }
}

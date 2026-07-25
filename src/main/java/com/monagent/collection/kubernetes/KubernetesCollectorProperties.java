package com.monagent.collection.kubernetes;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "monagent.collectors.kubernetes")
public record KubernetesCollectorProperties(
        @NotBlank String endpoint,
        @NotNull Duration timeout,
        @Min(1) int windowMinutes,
        boolean inCluster,
        URI apiServerBaseUrl,
        String bearerToken,
        String kubeConfigPath) {

    public URI resolvedBaseUrl() {
        if (inCluster) {
            return apiServerBaseUrl != null ? apiServerBaseUrl : URI.create("https://kubernetes.default.svc");
        }
        if (apiServerBaseUrl != null) {
            return apiServerBaseUrl;
        }
        return URI.create("http://localhost:8001");
    }
}

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
            if (!isSecure(apiServerBaseUrl)) {
                throw new IllegalArgumentException("Kubernetes API server baseUrl must be https or local");
            }
            return apiServerBaseUrl;
        }
        return URI.create("http://localhost:8001");
    }

    private static boolean isSecure(URI uri) {
        return "https".equalsIgnoreCase(uri.getScheme()) || isLocal(uri);
    }

    private static boolean isLocal(URI uri) {
        String host = uri.getHost();
        return host != null && (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("::1"));
    }
}

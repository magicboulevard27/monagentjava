package com.monagent.collection.logs;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "monagent.collectors.logs")
public record LogAnalyzerProperties(
        @NotNull URI baseUrl,
        @NotBlank String endpoint,
        @NotNull Duration timeout,
        @Min(1) int windowMinutes,
        @Min(1) int retryAttempts,
        @Min(1) int circuitBreakerFailureThreshold,
        @Min(1) int bulkheadMaxConcurrentCalls,
        String bearerToken,
        String basicAuthUsername,
        String basicAuthPassword) {

    public LogAnalyzerProperties {
        boolean hasAuth = hasText(bearerToken) || hasText(basicAuthUsername) || hasText(basicAuthPassword);
        if (baseUrl != null && hasAuth && !isSecure(baseUrl)) {
            throw new IllegalArgumentException("Log search auth requires an https baseUrl");
        }
    }

    private static boolean isSecure(URI uri) {
        return "https".equalsIgnoreCase(uri.getScheme()) || isLocal(uri);
    }

    private static boolean isLocal(URI uri) {
        String host = uri.getHost();
        return host != null && (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("::1"));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

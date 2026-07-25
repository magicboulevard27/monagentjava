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
        String bearerToken,
        String basicAuthUsername,
        String basicAuthPassword) {

    public LogAnalyzerProperties {
        boolean hasAuth = hasText(bearerToken) || hasText(basicAuthUsername) || hasText(basicAuthPassword);
        if (baseUrl != null && hasAuth && !"https".equalsIgnoreCase(baseUrl.getScheme())) {
            throw new IllegalArgumentException("Log search auth requires an https baseUrl");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

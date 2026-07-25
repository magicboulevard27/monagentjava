package com.monagent.collection.prometheus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "monagent.collectors.prometheus")
public record PrometheusCollectorProperties(
        @NotNull Duration timeout,
        @NotNull URI baseUrl,
        String bearerToken,
        String basicAuthUsername,
        String basicAuthPassword,
        @NotEmpty List<@Valid MetricQuery> queries) {

    public PrometheusCollectorProperties {
        boolean hasAuth = hasText(bearerToken) || hasText(basicAuthUsername) || hasText(basicAuthPassword);
        if (baseUrl != null && !"https".equalsIgnoreCase(baseUrl.getScheme()) && hasAuth) {
            throw new IllegalArgumentException("Prometheus auth requires an https baseUrl");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record MetricQuery(
            @NotBlank String name,
            @NotBlank String promql,
            @NotBlank String unit) {
    }
}

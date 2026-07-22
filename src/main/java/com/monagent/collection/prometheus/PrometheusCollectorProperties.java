package com.monagent.collection.prometheus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "monagent.collectors.prometheus")
public record PrometheusCollectorProperties(
        @NotNull Duration timeout,
        @NotEmpty List<@Valid MetricQuery> queries) {

    public record MetricQuery(
            @NotBlank String name,
            @NotBlank String promql,
            @NotBlank String unit) {
    }
}

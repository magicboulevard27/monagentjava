package com.monagent.collection.health;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "monagent.collectors.health")
public record HealthCollectorProperties(
        @NotNull Duration timeout,
        @Min(1) @Max(3600) long intervalSeconds,
        @Min(0) @Max(10) int retryCount) {
}

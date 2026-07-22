package com.monagent.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "monagent.async")
public record AsyncProcessingProperties(
        @NotNull Duration dispatchTimeout,
        @Min(1) int workerThreads,
        @Min(1) int queueCapacity,
        @Min(1) int collectorPoolSize) {
}

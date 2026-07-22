package com.monagent.collection.kubernetes;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "monagent.collectors.kubernetes")
public record KubernetesCollectorProperties(
        @NotBlank String endpoint,
        @NotNull Duration timeout,
        @Min(1) int windowMinutes) {
}

package com.monagent.collection.logs;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "monagent.collectors.logs")
public record LogAnalyzerProperties(
        @NotBlank String endpoint,
        @NotNull Duration timeout,
        @Min(1) int windowMinutes) {
}

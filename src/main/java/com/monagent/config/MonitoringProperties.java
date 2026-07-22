package com.monagent.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "monitoring")
public record MonitoringProperties(
        @NotBlank String applicationName,
        @NotBlank String defaultTimezone) {
}


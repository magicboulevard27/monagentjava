package com.monagent.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "monagent.runtime")
public record RuntimeProperties(
        @NotBlank String timezone,
        @NotBlank String logDirectory,
        @NotBlank String dataDirectory) {
}


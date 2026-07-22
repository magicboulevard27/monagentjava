package com.monagent.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "monagent.reload")
public record ReloadableConfigurationProperties(
        @NotBlank String file) {
}

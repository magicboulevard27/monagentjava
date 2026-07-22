package com.monagent.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "monagent.integrations")
public record IntegrationProperties(
        @Valid @NotNull Database database,
        @Valid @NotNull Ollama ollama,
        @Valid @NotNull Notifications notifications,
        @Valid @NotNull Auth auth,
        @Valid @NotNull Observability observability) {

    public record Database(
            @NotBlank String url,
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String schema) {
    }

    public record Ollama(
            @NotNull URI baseUrl,
            @NotBlank String model,
            @NotNull Integer requestTimeoutSeconds) {
    }

    public record Notifications(
            @NotEmpty List<@NotBlank String> enabledChannels,
            @NotBlank String fromAddress) {
    }

    public record Auth(
            @NotBlank String issuerUri,
            @NotBlank String audience,
            @NotBlank String defaultRoleClaim) {
    }

    public record Observability(
            @NotBlank String metricsExportEndpoint,
            @NotBlank String tracingExportEndpoint) {
    }
}

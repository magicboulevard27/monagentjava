package com.monagent.config;

public record ConfigurationSnapshot(
        RuntimeProperties runtime,
        IntegrationProperties integrations) {
}

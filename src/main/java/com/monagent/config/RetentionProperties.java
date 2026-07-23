package com.monagent.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "monagent.retention")
public record RetentionProperties(
        Duration monitoringSignals,
        Duration incidentEvidence,
        Duration auditLogs,
        String archiveLocation) {
}

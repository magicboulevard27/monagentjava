package com.monagent.collection.kubernetes;

import java.time.Instant;
import java.util.UUID;

public record DeploymentContextFinding(
        UUID serviceId,
        String serviceName,
        String environment,
        String resourceKind,
        String eventType,
        String message,
        Instant observedAt,
        String rawReference) {
}

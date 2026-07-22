package com.monagent.collection.model;

import java.time.Instant;
import java.util.UUID;

public record HealthSourceSignal(
        UUID serviceId,
        String serviceName,
        String environment,
        Instant observedAt,
        String healthState,
        boolean dependencyIssue,
        String rawReference) implements SourceSignal {
}

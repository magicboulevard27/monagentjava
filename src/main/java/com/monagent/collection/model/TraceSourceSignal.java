package com.monagent.collection.model;

import java.time.Instant;
import java.util.UUID;

public record TraceSourceSignal(
        UUID serviceId,
        String serviceName,
        String environment,
        Instant observedAt,
        String spanName,
        long durationMillis,
        String status,
        String dependencyName,
        String rawReference) implements SourceSignal {
}

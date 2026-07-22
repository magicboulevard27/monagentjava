package com.monagent.collection.traces;

import java.time.Instant;
import java.util.UUID;

public record TraceFinding(
        UUID serviceId,
        String serviceName,
        String environment,
        String spanName,
        long durationMillis,
        String status,
        String dependencyName,
        String summary,
        Instant observedAt,
        String rawReference) {
}

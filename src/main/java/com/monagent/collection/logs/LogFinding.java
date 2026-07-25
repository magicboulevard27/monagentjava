package com.monagent.collection.logs;

import java.time.Instant;
import java.util.UUID;

public record LogFinding(
        UUID serviceId,
        String serviceName,
        String environment,
        String pattern,
        String summary,
        long occurrenceCount,
        Instant timestamp,
        String correlationId,
        String exceptionType,
        Instant observedAt,
        String rawReference) {
}

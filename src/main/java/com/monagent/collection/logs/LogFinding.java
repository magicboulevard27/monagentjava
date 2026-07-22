package com.monagent.collection.logs;

import java.time.Instant;
import java.util.UUID;

public record LogFinding(
        UUID serviceId,
        String serviceName,
        String environment,
        String pattern,
        String summary,
        Instant observedAt,
        String rawReference) {
}

package com.monagent.collection.model;

import java.time.Instant;
import java.util.UUID;

public record LogSourceSignal(
        UUID serviceId,
        String serviceName,
        String environment,
        Instant observedAt,
        String pattern,
        String message,
        String rawReference) implements SourceSignal {
}

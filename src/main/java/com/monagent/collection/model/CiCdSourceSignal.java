package com.monagent.collection.model;

import java.time.Instant;
import java.util.UUID;

public record CiCdSourceSignal(
        UUID serviceId,
        String serviceName,
        String environment,
        Instant observedAt,
        String changeType,
        String revision,
        String rawReference) implements SourceSignal {
}

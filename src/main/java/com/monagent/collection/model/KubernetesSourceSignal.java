package com.monagent.collection.model;

import java.time.Instant;
import java.util.UUID;

public record KubernetesSourceSignal(
        UUID serviceId,
        String serviceName,
        String environment,
        Instant observedAt,
        String resourceKind,
        String eventType,
        String message,
        String rawReference) implements SourceSignal {
}

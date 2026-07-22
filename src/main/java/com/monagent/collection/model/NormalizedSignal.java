package com.monagent.collection.model;

import java.time.Instant;
import java.util.UUID;

public record NormalizedSignal(
        UUID signalId,
        UUID serviceId,
        SourceType sourceType,
        String signalName,
        String signalValue,
        String unit,
        SignalStatus status,
        SignalSeverity severity,
        Instant collectedAt,
        String rawReference) {
}

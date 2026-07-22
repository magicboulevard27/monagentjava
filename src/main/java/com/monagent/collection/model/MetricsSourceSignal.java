package com.monagent.collection.model;

import java.time.Instant;
import java.util.UUID;

public record MetricsSourceSignal(
        UUID serviceId,
        String serviceName,
        String environment,
        Instant observedAt,
        String metricName,
        double value,
        String unit,
        String rawReference) implements SourceSignal {
}

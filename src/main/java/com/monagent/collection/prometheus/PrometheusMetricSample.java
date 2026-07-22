package com.monagent.collection.prometheus;

import java.time.Instant;
import java.util.UUID;

public record PrometheusMetricSample(
        UUID serviceId,
        String serviceName,
        String environment,
        Instant observedAt,
        String metricName,
        double value,
        String unit,
        String rawReference) {
}

package com.monagent.collection.traces;

import java.time.Duration;
import java.time.Instant;

public record TraceQuery(
        String serviceName,
        String operation,
        String status,
        Instant incidentWindowStart,
        Instant incidentWindowEnd,
        Duration timeout) {
}

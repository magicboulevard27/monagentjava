package com.monagent.api.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IncidentResponse(
        UUID incidentId,
        String title,
        String severity,
        String status,
        List<String> affectedServices,
        Instant startTime,
        Instant detectedAt,
        Instant resolvedAt,
        String likelyRootCause,
        String confidence,
        String summary) {
}

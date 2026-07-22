package com.monagent.analysis;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IncidentCandidate(
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
        String summary,
        List<IncidentEvidence> evidence) {
}

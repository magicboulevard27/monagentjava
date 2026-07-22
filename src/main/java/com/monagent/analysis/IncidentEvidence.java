package com.monagent.analysis;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record IncidentEvidence(
        UUID evidenceId,
        UUID incidentId,
        String sourceType,
        String serviceName,
        String evidenceType,
        String description,
        Instant observedAt,
        String referenceId,
        Map<String, Object> redactedPayload) {
}

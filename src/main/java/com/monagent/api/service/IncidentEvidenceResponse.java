package com.monagent.api.service;

import java.time.Instant;
import java.util.UUID;

public record IncidentEvidenceResponse(
        UUID evidenceId,
        UUID incidentId,
        String sourceType,
        String serviceName,
        String evidenceType,
        String description,
        Instant observedAt,
        String referenceId) {
}

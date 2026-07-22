package com.monagent.api.service;

import java.time.Instant;
import java.util.UUID;

public record RecommendationSummaryResponse(
        UUID recommendationId,
        UUID incidentId,
        String actionType,
        String description,
        String riskLevel,
        boolean requiresApproval,
        String status,
        String evidenceSummary,
        Instant createdAt) {
}

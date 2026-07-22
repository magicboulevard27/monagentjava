package com.monagent.analysis;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Recommendation(
        UUID recommendationId,
        UUID incidentId,
        RecommendationActionType actionType,
        String description,
        RecommendationRiskLevel riskLevel,
        boolean requiresApproval,
        String status,
        String evidenceSummary,
        List<String> evidenceIds,
        Instant createdAt) {
}

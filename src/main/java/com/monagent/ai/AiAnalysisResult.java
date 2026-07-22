package com.monagent.ai;

import java.util.List;

public record AiAnalysisResult(
        String incidentId,
        String severity,
        List<String> affectedServices,
        String status,
        List<String> symptoms,
        String likelyRootCause,
        String confidence,
        List<String> evidenceIds,
        List<String> recommendedActions,
        boolean escalate,
        String model,
        String promptVersion,
        long latencyMillis,
        int tokenUsage,
        String resultStatus) {
}

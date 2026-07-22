package com.monagent.ai;

import com.monagent.analysis.IncidentCandidate;
import com.monagent.analysis.IncidentEvidence;
import com.monagent.analysis.AnomalyOutcome;
import java.util.List;

public record AiAnalysisRequest(
        List<AnomalyOutcome> anomalies,
        List<IncidentEvidence> evidence,
        IncidentCandidate incidentCandidate,
        String deploymentContext) {
}

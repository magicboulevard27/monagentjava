package com.monagent.ai;

import com.monagent.analysis.AnomalyOutcome;
import com.monagent.analysis.IncidentCandidate;
import com.monagent.analysis.IncidentEvidence;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class IncidentAnalysisPromptBuilder {

    public static final String PROMPT_VERSION = "incident-ai-v1";
    private final SensitiveInputRedactor redactor;

    public IncidentAnalysisPromptBuilder(SensitiveInputRedactor redactor) {
        this.redactor = redactor;
    }

    public String build(AiAnalysisRequest request) {
        return """
                You are an incident analysis engine.
                Ground every claim in the supplied evidence only.
                Do not invent root causes, symptoms, or actions that are not supported.
                Return JSON only.

                Incident:
                %s

                Anomalies:
                %s

                Evidence:
                %s

                Deployment context:
                %s
                """.formatted(
                renderIncident(request.incidentCandidate()),
                renderAnomalies(request.anomalies()),
                renderEvidence(request.evidence()),
                redactor.redact(nullToEmpty(request.deploymentContext())));
    }

    private String renderIncident(IncidentCandidate incidentCandidate) {
        if (incidentCandidate == null) {
            return "none";
        }
        return "id=%s severity=%s status=%s services=%s summary=%s".formatted(
                incidentCandidate.incidentId(),
                incidentCandidate.severity(),
                incidentCandidate.status(),
                incidentCandidate.affectedServices(),
                incidentCandidate.summary());
    }

    private String renderAnomalies(List<AnomalyOutcome> anomalies) {
        if (anomalies == null || anomalies.isEmpty()) {
            return "none";
        }
        return anomalies.stream()
                .map(outcome -> "%s:%s:%s".formatted(outcome.metricName(), outcome.severity(), outcome.outcomeStatus()))
                .collect(Collectors.joining(", "));
    }

    private String renderEvidence(List<IncidentEvidence> evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return "none";
        }
        return evidence.stream()
                .map(item -> "%s:%s:%s".formatted(item.evidenceId(), redactor.redact(item.sourceType()), redactor.redact(item.description())))
                .collect(Collectors.joining(", "));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

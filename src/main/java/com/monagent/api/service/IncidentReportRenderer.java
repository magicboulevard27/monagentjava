package com.monagent.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class IncidentReportRenderer {

    private final ObjectMapper objectMapper;

    public IncidentReportRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String renderJson(IncidentQueryService.IncidentReportDocument document) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("incident", toIncidentMap(document.incident()));
            payload.put("evidence", document.evidence().stream().map(this::toEvidenceMap).toList());
            payload.put("recommendations", document.recommendations().stream().map(this::toRecommendationMap).toList());
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to render incident report as JSON", ex);
        }
    }

    private Map<String, Object> toIncidentMap(IncidentResponse incident) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("incidentId", incident.incidentId());
        payload.put("title", incident.title());
        payload.put("severity", incident.severity());
        payload.put("status", incident.status());
        payload.put("affectedServices", incident.affectedServices());
        payload.put("startTime", formatInstant(incident.startTime()));
        payload.put("detectedAt", formatInstant(incident.detectedAt()));
        payload.put("resolvedAt", formatInstant(incident.resolvedAt()));
        payload.put("likelyRootCause", incident.likelyRootCause());
        payload.put("confidence", incident.confidence());
        payload.put("summary", incident.summary());
        return payload;
    }

    private Map<String, Object> toEvidenceMap(IncidentEvidenceQueryResponse evidence) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("evidenceId", evidence.evidenceId());
        payload.put("incidentId", evidence.incidentId());
        payload.put("sourceType", evidence.sourceType());
        payload.put("serviceName", evidence.serviceName());
        payload.put("evidenceType", evidence.evidenceType());
        payload.put("description", evidence.description());
        payload.put("observedAt", formatInstant(evidence.observedAt()));
        payload.put("referenceId", evidence.referenceId());
        return payload;
    }

    private Map<String, Object> toRecommendationMap(RecommendationSummaryResponse recommendation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recommendationId", recommendation.recommendationId());
        payload.put("incidentId", recommendation.incidentId());
        payload.put("actionType", recommendation.actionType());
        payload.put("description", recommendation.description());
        payload.put("riskLevel", recommendation.riskLevel());
        payload.put("requiresApproval", recommendation.requiresApproval());
        payload.put("status", recommendation.status());
        payload.put("evidenceSummary", recommendation.evidenceSummary());
        payload.put("createdAt", formatInstant(recommendation.createdAt()));
        return payload;
    }

    private String formatInstant(Instant value) {
        return value == null ? null : value.toString();
    }

    public String renderMarkdown(IncidentQueryService.IncidentReportDocument document) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Incident ").append(document.incident().incidentId()).append('\n');
        builder.append("Severity: ").append(document.incident().severity()).append('\n');
        builder.append("Status: ").append(document.incident().status()).append('\n');
        builder.append("Services: ").append(String.join(", ", document.incident().affectedServices())).append("\n\n");
        if (document.evidence().isEmpty()) {
            builder.append("> Degradation note: supporting evidence was not available at report time.\n\n");
        }
        builder.append("## Summary\n").append(document.incident().summary()).append("\n\n");
        builder.append("## Evidence\n");
        for (IncidentEvidenceQueryResponse evidence : document.evidence()) {
            builder.append("- ").append(evidence.evidenceType()).append(": ").append(evidence.description()).append('\n');
        }
        builder.append("\n## Recommendations\n");
        for (RecommendationSummaryResponse recommendation : document.recommendations()) {
            builder.append("- ").append(recommendation.actionType()).append(": ").append(recommendation.description()).append('\n');
        }
        return builder.toString();
    }
}

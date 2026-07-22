package com.monagent.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
            payload.put("incident", document.incident());
            payload.put("evidence", document.evidence());
            payload.put("recommendations", document.recommendations());
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to render incident report as JSON", ex);
        }
    }

    public String renderMarkdown(IncidentQueryService.IncidentReportDocument document) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Incident ").append(document.incident().incidentId()).append('\n');
        builder.append("Severity: ").append(document.incident().severity()).append('\n');
        builder.append("Status: ").append(document.incident().status()).append('\n');
        builder.append("Services: ").append(String.join(", ", document.incident().affectedServices())).append("\n\n");
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

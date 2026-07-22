package com.monagent.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class IncidentAnalysisResultParser {

    private final ObjectMapper objectMapper;

    public IncidentAnalysisResultParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedIncidentAnalysis parse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            return new ParsedIncidentAnalysis(
                    text(root, "incidentId"),
                    text(root, "severity"),
                    readList(root, "affectedServices"),
                    text(root, "status"),
                    readList(root, "symptoms"),
                    text(root, "likelyRootCause"),
                    text(root, "confidence"),
                    readList(root, "evidenceIds"),
                    readList(root, "recommendedActions"),
                    root.path("escalate").asBoolean(false));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid incident analysis JSON", ex);
        }
    }

    public List<String> validateEvidenceReferences(ParsedIncidentAnalysis analysis, List<String> availableEvidenceIds) {
        if (analysis.evidenceIds().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> missing = new ArrayList<>();
        for (String evidenceId : analysis.evidenceIds()) {
            if (!availableEvidenceIds.contains(evidenceId)) {
                missing.add(evidenceId);
            }
        }
        return missing;
    }

    private String text(JsonNode root, String field) {
        return root.path(field).asText("");
    }

    private List<String> readList(JsonNode root, String field) {
        JsonNode node = root.path(field);
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> values.add(item.asText()));
        return List.copyOf(values);
    }

    public record ParsedIncidentAnalysis(
            String incidentId,
            String severity,
            List<String> affectedServices,
            String status,
            List<String> symptoms,
            String likelyRootCause,
            String confidence,
            List<String> evidenceIds,
            List<String> recommendedActions,
            boolean escalate) {
    }
}

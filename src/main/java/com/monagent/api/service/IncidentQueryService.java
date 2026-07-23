package com.monagent.api.service;

import com.monagent.persistence.IncidentEntity;
import com.monagent.persistence.IncidentEvidenceEntity;
import com.monagent.persistence.IncidentEvidenceRepository;
import com.monagent.persistence.IncidentRepository;
import com.monagent.persistence.RecommendationEntity;
import com.monagent.persistence.RecommendationRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IncidentQueryService {

    private static final Logger log = LoggerFactory.getLogger(IncidentQueryService.class);

    private final IncidentRepository incidentRepository;
    private final IncidentEvidenceRepository incidentEvidenceRepository;
    private final RecommendationRepository recommendationRepository;
    private final IncidentReportRenderer incidentReportRenderer;

    public IncidentQueryService(IncidentRepository incidentRepository,
                                IncidentEvidenceRepository incidentEvidenceRepository,
                                RecommendationRepository recommendationRepository,
                                IncidentReportRenderer incidentReportRenderer) {
        this.incidentRepository = incidentRepository;
        this.incidentEvidenceRepository = incidentEvidenceRepository;
        this.recommendationRepository = recommendationRepository;
        this.incidentReportRenderer = incidentReportRenderer;
    }

    public List<IncidentResponse> list(String severity, String status, Integer limit, Integer offset) {
        int safeLimit = limit == null ? 50 : Math.min(Math.max(limit, 1), 200);
        int safeOffset = offset == null ? 0 : Math.max(offset, 0);
        return incidentRepository.findAll().stream()
                .filter(incident -> severity == null || severity.equalsIgnoreCase(incident.getSeverity()))
                .filter(incident -> status == null || status.equalsIgnoreCase(incident.getStatus()))
                .sorted(Comparator.comparing(IncidentEntity::getDetectedAt).reversed())
                .skip(safeOffset)
                .limit(safeLimit)
                .map(this::toResponse)
                .toList();
    }

    public IncidentResponse get(UUID incidentId) {
        return toResponse(incidentRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Incident not found: " + incidentId)));
    }

    public List<IncidentEvidenceQueryResponse> evidence(UUID incidentId) {
        ensureIncidentExists(incidentId);
        return incidentEvidenceRepository.findAll().stream()
                .filter(evidence -> incidentId.equals(evidence.getIncidentId()))
                .sorted(Comparator.comparing(IncidentEvidenceEntity::getObservedAt))
                .map(this::toEvidenceResponse)
                .toList();
    }

    public List<RecommendationSummaryResponse> recommendations(UUID incidentId) {
        ensureIncidentExists(incidentId);
        return recommendationRepository.findAll().stream()
                .filter(recommendation -> incidentId.equals(recommendation.getIncidentId()))
                .sorted(Comparator.comparing(RecommendationEntity::getCreatedAt))
                .map(this::toRecommendationResponse)
                .toList();
    }

    public IncidentReportResponse reportJson(UUID incidentId) {
        IncidentReportDocument document = buildReport(incidentId);
        return new IncidentReportResponse("json", incidentReportRenderer.renderJson(document));
    }

    public IncidentReportResponse reportMarkdown(UUID incidentId) {
        IncidentReportDocument document = buildReport(incidentId);
        return new IncidentReportResponse("markdown", incidentReportRenderer.renderMarkdown(document));
    }

    private IncidentReportDocument buildReport(UUID incidentId) {
        IncidentResponse incident = get(incidentId);
        List<IncidentEvidenceQueryResponse> evidence = evidence(incidentId);
        List<RecommendationSummaryResponse> recommendations = recommendations(incidentId);
        return new IncidentReportDocument(incident, evidence, recommendations);
    }

    private void ensureIncidentExists(UUID incidentId) {
        incidentRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Incident not found: " + incidentId));
    }

    private IncidentResponse toResponse(IncidentEntity entity) {
        return new IncidentResponse(
                entity.getIncidentId(),
                entity.getTitle(),
                entity.getSeverity(),
                entity.getStatus(),
                parseList(entity.getAffectedServices()),
                entity.getStartTime(),
                entity.getDetectedAt(),
                entity.getResolvedAt(),
                entity.getLikelyRootCause(),
                entity.getConfidence(),
                entity.getSummary());
    }

    private IncidentEvidenceQueryResponse toEvidenceResponse(IncidentEvidenceEntity entity) {
        return new IncidentEvidenceQueryResponse(
                entity.getEvidenceId(),
                entity.getIncidentId(),
                entity.getSourceType(),
                entity.getServiceName(),
                entity.getEvidenceType(),
                entity.getDescription(),
                entity.getObservedAt(),
                entity.getReferenceId());
    }

    private RecommendationSummaryResponse toRecommendationResponse(RecommendationEntity entity) {
        return new RecommendationSummaryResponse(
                entity.getRecommendationId(),
                entity.getIncidentId(),
                entity.getActionType(),
                entity.getDescription(),
                entity.getRiskLevel(),
                entity.isRequiresApproval(),
                entity.getStatus(),
                entity.getEvidenceSummary(),
                entity.getCreatedAt());
    }

    private List<String> parseList(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return List.of();
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String part : trimmed.split(",")) {
            values.add(part.trim().replace("\"", ""));
        }
        return List.copyOf(values);
    }

    public record IncidentReportDocument(
            IncidentResponse incident,
            List<IncidentEvidenceQueryResponse> evidence,
            List<RecommendationSummaryResponse> recommendations) {
    }
}

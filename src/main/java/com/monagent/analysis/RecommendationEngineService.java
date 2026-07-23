package com.monagent.analysis;

import com.monagent.persistence.RecommendationEntity;
import com.monagent.persistence.RecommendationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationEngineService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationEngineService.class);

    private final RecommendationRepository recommendationRepository;

    public RecommendationEngineService(RecommendationRepository recommendationRepository) {
        this.recommendationRepository = recommendationRepository;
    }

    @Transactional
    public List<Recommendation> generate(IncidentCandidate incident, List<IncidentEvidence> evidence) {
        List<Recommendation> recommendations = new ArrayList<>();
        if (incident == null) {
            return recommendations;
        }

        boolean hasEvidence = evidence != null && !evidence.isEmpty();
        String evidenceSummary = summarizeEvidence(evidence);
        List<String> evidenceIds = evidence == null ? List.of() : evidence.stream().map(item -> item.evidenceId().toString()).toList();

        if (!hasEvidence) {
            recommendations.add(persist(new Recommendation(
                    UUID.randomUUID(),
                    incident.incidentId(),
                    RecommendationActionType.NO_OP,
                    "No supporting evidence was provided; continue passive monitoring.",
                    RecommendationRiskLevel.LOW,
                    false,
                    "SUPPRESSED",
                    "No evidence available",
                    evidenceIds,
                    Instant.now())));
            return recommendations;
        }

        for (Recommendation recommendation : mapRecommendations(incident, evidenceSummary, evidenceIds)) {
            recommendations.add(persist(recommendation));
        }
        return recommendations;
    }

    public RecommendationSafetyDecision validateSafety(Recommendation recommendation) {
        boolean unsafe = recommendation.actionType() == RecommendationActionType.NO_OP
                || recommendation.description().toLowerCase().contains("delete")
                || recommendation.description().toLowerCase().contains("drop")
                || recommendation.description().toLowerCase().contains("terminate");
        return new RecommendationSafetyDecision(!unsafe, unsafe ? "Unsupported or unsafe action" : "Approved");
    }

    private List<Recommendation> mapRecommendations(IncidentCandidate incident, String evidenceSummary, List<String> evidenceIds) {
        List<Recommendation> mapped = new ArrayList<>();
        String joinedEvidence = evidenceSummary.isBlank() ? "No evidence summary available" : evidenceSummary;

        if ("CRITICAL".equalsIgnoreCase(incident.severity()) || "HIGH".equalsIgnoreCase(incident.severity())) {
            mapped.add(new Recommendation(UUID.randomUUID(), incident.incidentId(),
                    RecommendationActionType.RESTART_SERVICE,
                    "Restart the affected service after confirming current state.",
                    RecommendationRiskLevel.HIGH,
                    true,
                    "PENDING",
                    joinedEvidence,
                    evidenceIds,
                    Instant.now()));
        }

        if (incident.summary() != null && incident.summary().toLowerCase().contains("cpu")) {
            mapped.add(new Recommendation(UUID.randomUUID(), incident.incidentId(),
                    RecommendationActionType.SCALE_UP,
                    "Scale compute resources for the affected workload.",
                    RecommendationRiskLevel.MEDIUM,
                    true,
                    "PENDING",
                    joinedEvidence,
                    evidenceIds,
                    Instant.now()));
            mapped.add(new Recommendation(UUID.randomUUID(), incident.incidentId(),
                    RecommendationActionType.ADJUST_RESOURCE_LIMITS,
                    "Review CPU and memory resource limits for the workload.",
                    RecommendationRiskLevel.MEDIUM,
                    false,
                    "PENDING",
                    joinedEvidence,
                    evidenceIds,
                    Instant.now()));
        }

        if (incident.summary() != null && incident.summary().toLowerCase().contains("memory")) {
            mapped.add(new Recommendation(UUID.randomUUID(), incident.incidentId(),
                    RecommendationActionType.SCALE_UP,
                    "Increase memory headroom for the workload.",
                    RecommendationRiskLevel.MEDIUM,
                    true,
                    "PENDING",
                    joinedEvidence,
                    evidenceIds,
                    Instant.now()));
        }

        if (incident.summary() != null && incident.summary().toLowerCase().contains("db.pool")) {
            mapped.add(new Recommendation(UUID.randomUUID(), incident.incidentId(),
                    RecommendationActionType.SCALE_DATABASE_POOL,
                    "Increase database connection pool capacity after checking backend limits.",
                    RecommendationRiskLevel.HIGH,
                    true,
                    "PENDING",
                    joinedEvidence,
                    evidenceIds,
                    Instant.now()));
        }

        if (incident.summary() != null && incident.summary().toLowerCase().contains("kafka")) {
            mapped.add(new Recommendation(UUID.randomUUID(), incident.incidentId(),
                    RecommendationActionType.INVESTIGATE_KAFKA_LAG,
                    "Inspect Kafka consumer lag and partition assignment health.",
                    RecommendationRiskLevel.MEDIUM,
                    false,
                    "PENDING",
                    joinedEvidence,
                    evidenceIds,
                    Instant.now()));
        }

        if (incident.likelyRootCause() != null && incident.likelyRootCause().toLowerCase().contains("dependency")) {
            mapped.add(new Recommendation(UUID.randomUUID(), incident.incidentId(),
                    RecommendationActionType.CHECK_DEPENDENCY,
                    "Inspect downstream dependency health before taking corrective action.",
                    RecommendationRiskLevel.MEDIUM,
                    false,
                    "PENDING",
                    joinedEvidence,
                    evidenceIds,
                    Instant.now()));
        }

        if (incident.summary() != null && incident.summary().toLowerCase().contains("deployment")) {
            mapped.add(new Recommendation(UUID.randomUUID(), incident.incidentId(),
                    RecommendationActionType.ROLLBACK_DEPLOYMENT,
                    "Rollback the latest deployment if the symptom started after release validation.",
                    RecommendationRiskLevel.CRITICAL,
                    true,
                    "PENDING",
                    joinedEvidence,
                    evidenceIds,
                    Instant.now()));
        }

        if (mapped.isEmpty()) {
            mapped.add(new Recommendation(UUID.randomUUID(), incident.incidentId(),
                    RecommendationActionType.REVIEW_CONFIGURATION,
                    "Review configuration and dependency health using the correlated evidence.",
                    RecommendationRiskLevel.LOW,
                    false,
                    "PENDING",
                    joinedEvidence,
                    evidenceIds,
                    Instant.now()));
        }

        return mapped;
    }

    private Recommendation persist(Recommendation recommendation) {
        RecommendationEntity entity = new RecommendationEntity();
        entity.setRecommendationId(recommendation.recommendationId());
        entity.setIncidentId(recommendation.incidentId());
        entity.setActionType(recommendation.actionType().name());
        entity.setDescription(recommendation.description());
        entity.setRiskLevel(recommendation.riskLevel().name());
        entity.setRequiresApproval(recommendation.requiresApproval());
        entity.setStatus(recommendation.status());
        entity.setEvidenceSummary(recommendation.evidenceSummary());
        entity.setCreatedAt(recommendation.createdAt());
        recommendationRepository.saveAndFlush(entity);
        return recommendation;
    }

    private String summarizeEvidence(List<IncidentEvidence> evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return "";
        }
        return evidence.stream()
                .map(item -> item.sourceType() + ":" + item.description())
                .reduce((left, right) -> left + "; " + right)
                .orElse("");
    }
}

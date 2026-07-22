package com.monagent.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.monagent.persistence.RecommendationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecommendationEngineServiceTest {

    @Test
    void generatesRestartAndScalingRecommendationsFromIncidentEvidence() {
        RecommendationRepository repository = mock(RecommendationRepository.class);
        RecommendationEngineService service = new RecommendationEngineService(repository);

        IncidentEvidence evidence = new IncidentEvidence(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ANOMALY",
                "cpu",
                "GREATER_THAN",
                "cpu utilization high",
                Instant.parse("2026-07-22T10:00:00Z"),
                "ref-1",
                Map.of("metric", "cpu"));
        IncidentCandidate incident = new IncidentCandidate(
                UUID.randomUUID(),
                "HIGH incident on cpu",
                "HIGH",
                "ACTIVE",
                List.of("service-a"),
                Instant.parse("2026-07-22T10:00:00Z"),
                Instant.parse("2026-07-22T10:05:00Z"),
                null,
                "Correlated anomaly in cpu",
                "HIGH",
                "cpu saturation from deployment",
                List.of(evidence));

        List<Recommendation> recommendations = service.generate(incident, List.of(evidence));

        assertThat(recommendations).isNotEmpty();
        assertThat(recommendations).extracting(Recommendation::actionType)
                .contains(RecommendationActionType.RESTART_SERVICE, RecommendationActionType.SCALE_UP, RecommendationActionType.ADJUST_RESOURCE_LIMITS);
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsUnsafeActions() {
        RecommendationRepository repository = mock(RecommendationRepository.class);
        RecommendationEngineService service = new RecommendationEngineService(repository);
        RecommendationSafetyDecision decision = service.validateSafety(new Recommendation(
                UUID.randomUUID(),
                UUID.randomUUID(),
                RecommendationActionType.RESTART_SERVICE,
                "delete all data",
                RecommendationRiskLevel.CRITICAL,
                true,
                "PENDING",
                "evidence",
                List.of("e-1"),
                Instant.now()));

        assertThat(decision.safe()).isFalse();
        assertThat(decision.reason()).contains("unsafe");
    }

    @Test
    void createsFallbackRecommendationWhenNoEvidenceExists() {
        RecommendationRepository repository = mock(RecommendationRepository.class);
        RecommendationEngineService service = new RecommendationEngineService(repository);

        List<Recommendation> recommendations = service.generate(
                new IncidentCandidate(
                        UUID.randomUUID(),
                        "LOW incident",
                        "LOW",
                        "ACTIVE",
                        List.of("service-a"),
                        Instant.parse("2026-07-22T10:00:00Z"),
                        Instant.parse("2026-07-22T10:05:00Z"),
                        null,
                        "Unknown",
                        "LOW",
                        "summary",
                        List.of()),
                List.of());

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.getFirst().actionType()).isEqualTo(RecommendationActionType.NO_OP);
    }
}

package com.monagent.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monagent.persistence.IncidentEvidenceRepository;
import com.monagent.persistence.IncidentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IncidentCorrelationServiceTest {

    @Test
    void correlatesAnomaliesIntoIncidentAndPersistsEvidence() {
        IncidentRepository incidentRepository = mock(IncidentRepository.class);
        IncidentEvidenceRepository evidenceRepository = mock(IncidentEvidenceRepository.class);
        IncidentCorrelationService service = new IncidentCorrelationService(incidentRepository, evidenceRepository);

        AnomalyOutcome cpu = new AnomalyOutcome(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cpu",
                new BigDecimal("80"),
                new BigDecimal("91"),
                ThresholdComparator.GREATER_THAN,
                "HIGH",
                "TRIGGERED",
                5,
                3,
                Instant.parse("2026-07-22T10:00:00Z"),
                Instant.parse("2026-07-22T10:10:00Z"),
                List.of("cpu-ref"));
        AnomalyOutcome memory = new AnomalyOutcome(
                UUID.randomUUID(),
                cpu.serviceId(),
                UUID.randomUUID(),
                "memory",
                new BigDecimal("85"),
                new BigDecimal("90"),
                ThresholdComparator.GREATER_THAN,
                "HIGH",
                "TRIGGERED",
                5,
                3,
                Instant.parse("2026-07-22T10:05:00Z"),
                Instant.parse("2026-07-22T10:15:00Z"),
                List.of("memory-ref"));

        var candidate = service.correlate(List.of(cpu, memory));

        assertThat(candidate.severity()).isEqualTo("HIGH");
        assertThat(candidate.affectedServices()).containsExactly(cpu.serviceId().toString());
        assertThat(candidate.evidence()).hasSize(2);
        verify(incidentRepository).saveAndFlush(org.mockito.ArgumentMatchers.any());
        verify(evidenceRepository).saveAllAndFlush(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void mergesDuplicateCandidatesByStatusSeverityAndServiceSet() {
        IncidentRepository incidentRepository = mock(IncidentRepository.class);
        IncidentEvidenceRepository evidenceRepository = mock(IncidentEvidenceRepository.class);
        IncidentCorrelationService service = new IncidentCorrelationService(incidentRepository, evidenceRepository);

        UUID incidentId = UUID.randomUUID();
        IncidentCandidate first = new IncidentCandidate(
                incidentId,
                "HIGH incident on cpu",
                "HIGH",
                "ACTIVE",
                List.of("service-a"),
                Instant.parse("2026-07-22T10:00:00Z"),
                Instant.parse("2026-07-22T10:05:00Z"),
                null,
                "Correlated anomaly in cpu",
                "HIGH",
                "cpu=91",
                List.of());
        IncidentCandidate second = new IncidentCandidate(
                UUID.randomUUID(),
                "HIGH incident on cpu",
                "HIGH",
                "ACTIVE",
                List.of("service-a"),
                Instant.parse("2026-07-22T10:00:00Z"),
                Instant.parse("2026-07-22T10:08:00Z"),
                null,
                "Correlated anomaly in cpu",
                "HIGH",
                "cpu=94",
                List.of());

        List<IncidentCandidate> merged = service.mergeDuplicateCandidates(List.of(first, second));

        assertThat(merged).hasSize(1);
        assertThat(merged.getFirst().detectedAt()).isEqualTo(Instant.parse("2026-07-22T10:08:00Z"));
    }

    @Test
    void assessesImpactAndTransitionsLifecycleStates() {
        IncidentRepository incidentRepository = mock(IncidentRepository.class);
        IncidentEvidenceRepository evidenceRepository = mock(IncidentEvidenceRepository.class);
        IncidentCorrelationService service = new IncidentCorrelationService(incidentRepository, evidenceRepository);

        AnomalyOutcome first = new AnomalyOutcome(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cpu",
                new BigDecimal("80"),
                new BigDecimal("91"),
                ThresholdComparator.GREATER_THAN,
                "HIGH",
                "TRIGGERED",
                5,
                3,
                Instant.parse("2026-07-22T10:00:00Z"),
                Instant.parse("2026-07-22T10:10:00Z"),
                List.of("ref-1"));
        AnomalyOutcome second = new AnomalyOutcome(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "memory",
                new BigDecimal("85"),
                new BigDecimal("90"),
                ThresholdComparator.GREATER_THAN,
                "HIGH",
                "TRIGGERED",
                5,
                3,
                Instant.parse("2026-07-22T10:05:00Z"),
                Instant.parse("2026-07-22T10:15:00Z"),
                List.of("ref-2"));
        AnomalyOutcome third = new AnomalyOutcome(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "db.pool",
                new BigDecimal("90"),
                new BigDecimal("96"),
                ThresholdComparator.GREATER_THAN,
                "HIGH",
                "TRIGGERED",
                5,
                3,
                Instant.parse("2026-07-22T10:06:00Z"),
                Instant.parse("2026-07-22T10:16:00Z"),
                List.of("ref-3"));

        var impact = service.assessImpact(List.of(first, second, third), true);

        assertThat(impact.affectedServiceCount()).isEqualTo(3);
        assertThat(impact.blastRadiusElevated()).isTrue();
        assertThat(service.transition(null, false, false)).isEqualTo(IncidentLifecycleState.CANDIDATE);
        assertThat(service.transition(IncidentLifecycleState.ACTIVE, true, false)).isEqualTo(IncidentLifecycleState.RESOLVED);
        assertThat(service.transition(IncidentLifecycleState.ACTIVE, false, true)).isEqualTo(IncidentLifecycleState.SUPPRESSED);
        assertThat(service.transition(IncidentLifecycleState.MERGED, false, false)).isEqualTo(IncidentLifecycleState.MERGED);
    }
}

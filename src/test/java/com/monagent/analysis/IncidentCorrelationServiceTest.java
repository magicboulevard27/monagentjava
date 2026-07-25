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

    @Test
    void correlatesUpstreamAndDownstreamDependenciesIntoNarrative() {
        IncidentRepository incidentRepository = mock(IncidentRepository.class);
        IncidentEvidenceRepository evidenceRepository = mock(IncidentEvidenceRepository.class);
        IncidentCorrelationService service = new IncidentCorrelationService(incidentRepository, evidenceRepository);

        UUID upstreamService = UUID.randomUUID();
        UUID downstreamService = UUID.randomUUID();
        AnomalyOutcome upstream = new AnomalyOutcome(
                UUID.randomUUID(),
                upstreamService,
                UUID.randomUUID(),
                "checkout",
                new BigDecimal("80"),
                new BigDecimal("91"),
                ThresholdComparator.GREATER_THAN,
                "HIGH",
                "TRIGGERED",
                5,
                3,
                Instant.parse("2026-07-22T10:00:00Z"),
                Instant.parse("2026-07-22T10:10:00Z"),
                List.of("upstream-ref"));
        AnomalyOutcome downstream = new AnomalyOutcome(
                UUID.randomUUID(),
                downstreamService,
                UUID.randomUUID(),
                "payments",
                new BigDecimal("85"),
                new BigDecimal("93"),
                ThresholdComparator.GREATER_THAN,
                "HIGH",
                "TRIGGERED",
                5,
                3,
                Instant.parse("2026-07-22T10:05:00Z"),
                Instant.parse("2026-07-22T10:15:00Z"),
                List.of("downstream-ref"));

        var candidate = service.correlate(List.of(upstream, downstream));

        assertThat(candidate.summary()).contains("Upstream dependency impact from service");
        assertThat(candidate.likelyRootCause()).contains(upstreamService.toString());
        assertThat(candidate.likelyRootCause()).contains(downstreamService.toString());
    }

    @Test
    void correlatesSharedInfrastructureAndRepeatedPatterns() {
        IncidentRepository incidentRepository = mock(IncidentRepository.class);
        IncidentEvidenceRepository evidenceRepository = mock(IncidentEvidenceRepository.class);
        IncidentCorrelationService service = new IncidentCorrelationService(incidentRepository, evidenceRepository);

        UUID sharedService = UUID.randomUUID();
        AnomalyOutcome first = new AnomalyOutcome(
                UUID.randomUUID(),
                sharedService,
                UUID.randomUUID(),
                "db.pool",
                new BigDecimal("90"),
                new BigDecimal("94"),
                ThresholdComparator.GREATER_THAN,
                "HIGH",
                "TRIGGERED",
                5,
                3,
                Instant.parse("2026-07-22T10:00:00Z"),
                Instant.parse("2026-07-22T10:10:00Z"),
                List.of("log-ref-1"));
        AnomalyOutcome second = new AnomalyOutcome(
                UUID.randomUUID(),
                sharedService,
                UUID.randomUUID(),
                "db.pool",
                new BigDecimal("90"),
                new BigDecimal("95"),
                ThresholdComparator.GREATER_THAN,
                "HIGH",
                "TRIGGERED",
                5,
                3,
                Instant.parse("2026-07-22T10:05:00Z"),
                Instant.parse("2026-07-22T10:15:00Z"),
                List.of("log-ref-2"));

        var candidate = service.correlate(List.of(first, second));

        assertThat(candidate.summary()).contains("Repeated log pattern or shared infrastructure symptom");
        assertThat(candidate.likelyRootCause()).contains("Repeated log pattern or shared infrastructure symptom");
    }

    @Test
    void correlatesTracePathsWithDeploymentEvents() {
        IncidentRepository incidentRepository = mock(IncidentRepository.class);
        IncidentEvidenceRepository evidenceRepository = mock(IncidentEvidenceRepository.class);
        IncidentCorrelationService service = new IncidentCorrelationService(incidentRepository, evidenceRepository);

        UUID serviceId = UUID.randomUUID();
        AnomalyOutcome anomaly = new AnomalyOutcome(
                UUID.randomUUID(),
                serviceId,
                UUID.randomUUID(),
                "checkout",
                new BigDecimal("80"),
                new BigDecimal("92"),
                ThresholdComparator.GREATER_THAN,
                "HIGH",
                "TRIGGERED",
                5,
                3,
                Instant.parse("2026-07-22T10:20:00Z"),
                Instant.parse("2026-07-22T10:30:00Z"),
                List.of("trace-ref"));

        com.monagent.collection.traces.TraceFinding traceFinding = new com.monagent.collection.traces.TraceFinding(
                serviceId,
                "checkout",
                "production",
                "GET /checkout",
                2450L,
                "error",
                "payments",
                "trace path through payments",
                Instant.parse("2026-07-22T10:18:00Z"),
                "trace-ref");

        com.monagent.collection.kubernetes.DeploymentContextFinding deploymentFinding =
                new com.monagent.collection.kubernetes.DeploymentContextFinding(
                        serviceId,
                        "checkout",
                        "production",
                        "Deployment",
                        "rollout",
                        "deployment rollout completed",
                        Instant.parse("2026-07-22T10:10:00Z"),
                        "deploy-ref");

        var candidate = service.correlate(List.of(anomaly), List.of(traceFinding), List.of(deploymentFinding));

        assertThat(candidate.summary()).contains("Trace path through GET /checkout -> payments latency=2450ms");
        assertThat(candidate.summary()).contains("Symptoms began after deployment event rollout on Deployment");
        assertThat(candidate.likelyRootCause()).contains("Trace path through GET /checkout -> payments latency=2450ms");
    }
}

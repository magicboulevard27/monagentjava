package com.monagent.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.monagent.analysis.AnomalyOutcome;
import com.monagent.analysis.IncidentCandidate;
import com.monagent.analysis.IncidentEvidence;
import com.monagent.analysis.ThresholdComparator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IncidentAnalysisServiceTest {

    @Test
    void analyzesStructuredResponseAndValidatesEvidenceReferences() {
        String evidenceId = "00000000-0000-0000-0000-000000000001";
        String response = """
                {
                  "incidentId": "INC-2026-001",
                  "severity": "HIGH",
                  "affectedServices": ["service-a"],
                  "status": "ACTIVE",
                  "symptoms": ["cpu"],
                  "likelyRootCause": "Correlated anomaly in cpu",
                  "confidence": "MEDIUM",
                  "evidenceIds": ["%s"],
                  "recommendedActions": ["restart service"],
                  "escalate": true
                }
                """.formatted(evidenceId);

        IncidentAnalysisService service = new IncidentAnalysisService(
                new StubIncidentAnalysisClient(response),
                new IncidentAnalysisPromptBuilder(new SensitiveInputRedactor()),
                new IncidentAnalysisResultParser(new com.fasterxml.jackson.databind.ObjectMapper()));

        IncidentCandidate candidate = new IncidentCandidate(
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
                "cpu=91",
                List.of(new IncidentEvidence(UUID.fromString(evidenceId), UUID.randomUUID(), "ANOMALY", "cpu", "GREATER_THAN", "triggered", Instant.parse("2026-07-22T10:05:00Z"), "ref", java.util.Map.of())));
        AiAnalysisResult result = service.analyze(new AiAnalysisRequest(
                List.of(new AnomalyOutcome(
                        UUID.randomUUID(),
                        candidate.incidentId(),
                        UUID.randomUUID(),
                        "cpu",
                        new BigDecimal("80"),
                        new BigDecimal("91"),
                        ThresholdComparator.GREATER_THAN,
                        "HIGH",
                        "TRIGGERED",
                        5,
                        3,
                        Instant.parse("2026-07-22T10:05:00Z"),
                        Instant.parse("2026-07-22T10:15:00Z"),
                        List.of("ref-1"))),
                candidate.evidence(),
                candidate,
                "deployment token=abc123"));

        assertThat(result.severity()).isEqualTo("HIGH");
        assertThat(result.affectedServices()).containsExactly("service-a");
        assertThat(result.escalate()).isTrue();
        assertThat(result.resultStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void fallsBackWhenClientFails() {
        IncidentAnalysisService service = new IncidentAnalysisService(
                prompt -> { throw new RuntimeException("offline"); },
                new IncidentAnalysisPromptBuilder(new SensitiveInputRedactor()),
                new IncidentAnalysisResultParser(new com.fasterxml.jackson.databind.ObjectMapper()));

        AiAnalysisResult result = service.analyze(new AiAnalysisRequest(
                List.of(new AnomalyOutcome(
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
                        List.of("ref-1"))),
                List.of(),
                null,
                "api-key=secret"));

        assertThat(result.resultStatus()).isEqualTo("FALLBACK");
        assertThat(result.recommendedActions()).isNotEmpty();
        assertThat(result.severity()).isEqualTo("HIGH");
    }
}

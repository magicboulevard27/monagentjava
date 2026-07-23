package com.monagent.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.monagent.ai.IncidentAnalysisPromptBuilder;
import com.monagent.ai.IncidentAnalysisResultParser;
import com.monagent.ai.IncidentAnalysisService;
import com.monagent.ai.SensitiveInputRedactor;
import com.monagent.ai.StubIncidentAnalysisClient;
import com.monagent.analysis.AnomalyDetectionService;
import com.monagent.analysis.AnomalyOutcome;
import com.monagent.analysis.IncidentCandidate;
import com.monagent.analysis.IncidentCorrelationService;
import com.monagent.analysis.Recommendation;
import com.monagent.analysis.RecommendationEngineService;
import com.monagent.api.service.MonitoredServiceService;
import com.monagent.collection.SignalNormalizationService;
import com.monagent.collection.model.HealthSourceSignal;
import com.monagent.collection.model.NormalizedSignal;
import com.monagent.collection.model.SignalSeverity;
import com.monagent.collection.model.SignalStatus;
import com.monagent.collection.model.SourceType;
import com.monagent.domain.MonitoredService;
import com.monagent.notification.EmailNotificationChannel;
import com.monagent.notification.NotificationDispatcher;
import com.monagent.notification.NotificationTemplateRenderer;
import com.monagent.notification.SlackNotificationChannel;
import com.monagent.persistence.IncidentEvidenceRepository;
import com.monagent.persistence.IncidentRepository;
import com.monagent.persistence.MonitoringSignalRepository;
import com.monagent.persistence.RecommendationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MonitoringPipelineTest {

    @Test
    void runsCollectionToNotificationPipelineWithSanitizedEvidence() {
        SignalNormalizationService normalizationService = new SignalNormalizationService();
        NormalizedSignal normalizedSignal = normalizationService.fromHealth(
                new HealthSourceSignal(UUID.randomUUID(), "orders-api", "development", Instant.parse("2026-07-22T10:00:00Z"), "DOWN", true, "status=DOWN"));

        MonitoringSignalRepository signalRepository = mock(MonitoringSignalRepository.class);
        when(signalRepository.saveAndFlush(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        com.monagent.collection.MonitoringSignalPersistenceService persistenceService =
                new com.monagent.collection.MonitoringSignalPersistenceService(signalRepository);
        NormalizedSignal persisted = persistenceService.save(normalizedSignal);
        assertThat(persisted.signalName()).isEqualTo("service.health");

        AnomalyDetectionService anomalyDetectionService = new AnomalyDetectionService(mock(com.monagent.persistence.AnomalyOutcomeRepository.class));
        AnomalyOutcome anomaly = anomalyDetectionService.evaluate(persisted);
        assertThat(anomaly.outcomeStatus()).isEqualTo("TRIGGERED");

        IncidentRepository incidentRepository = mock(IncidentRepository.class);
        IncidentEvidenceRepository evidenceRepository = mock(IncidentEvidenceRepository.class);
        when(incidentRepository.saveAndFlush(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(evidenceRepository.saveAllAndFlush(org.mockito.ArgumentMatchers.anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        IncidentCorrelationService correlationService = new IncidentCorrelationService(incidentRepository, evidenceRepository);
        IncidentCandidate incident = correlationService.correlate(List.of(anomaly));
        assertThat(incident.severity()).isEqualTo("CRITICAL");

        RecommendationRepository recommendationRepository = mock(RecommendationRepository.class);
        when(recommendationRepository.saveAndFlush(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        RecommendationEngineService recommendationEngineService = new RecommendationEngineService(recommendationRepository);
        List<Recommendation> recommendations = recommendationEngineService.generate(incident, incident.evidence());
        assertThat(recommendations).isNotEmpty();

        NotificationTemplateRenderer renderer = new NotificationTemplateRenderer();
        NotificationDispatcher dispatcher = new NotificationDispatcher(
                List.of(new SlackNotificationChannel(renderer), new EmailNotificationChannel(renderer, new com.monagent.config.IntegrationProperties(
                        new com.monagent.config.IntegrationProperties.Database("jdbc:postgresql://localhost:5432/monagent", "monagent", "monagent", "public"),
                        new com.monagent.config.IntegrationProperties.Ollama(java.net.URI.create("http://localhost:11434"), "llama3.1:8b-instruct", 30),
                        new com.monagent.config.IntegrationProperties.Notifications(List.of("slack", "email"), "monagent@example.com"),
                        new com.monagent.config.IntegrationProperties.Auth("issuer", "audience", "roles"),
                        new com.monagent.config.IntegrationProperties.Observability("metrics", "traces")))),
                renderer,
                new com.monagent.web.SelfObservabilityMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));

        var deliveries = dispatcher.dispatch(incident, recommendations, List.of("slack"));
        assertThat(deliveries).hasSize(1);
        assertThat(deliveries.getFirst().delivered()).isTrue();
    }

    @Test
    void canAnalyzeIncidentWithStubbedLLMAndRedaction() {
        IncidentAnalysisService service = new IncidentAnalysisService(
                new StubIncidentAnalysisClient("""
                        {
                          "incidentId": "INC-PIPELINE-1",
                          "severity": "HIGH",
                          "affectedServices": ["orders-api"],
                          "status": "ACTIVE",
                          "symptoms": ["down"],
                          "likelyRootCause": "dependency",
                          "confidence": "HIGH",
                          "evidenceIds": ["00000000-0000-0000-0000-000000000001"],
                          "recommendedActions": ["restart service"],
                          "escalate": true
                        }
                        """),
                new IncidentAnalysisPromptBuilder(new SensitiveInputRedactor()),
                new IncidentAnalysisResultParser(new com.fasterxml.jackson.databind.ObjectMapper()),
                new com.monagent.web.SelfObservabilityMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));

        var result = service.analyze(new com.monagent.ai.AiAnalysisRequest(
                List.of(),
                List.of(),
                new IncidentCandidate(
                        UUID.randomUUID(),
                        "orders-api down",
                        "HIGH",
                        "ACTIVE",
                        List.of("orders-api"),
                        Instant.parse("2026-07-22T10:00:00Z"),
                        Instant.parse("2026-07-22T10:05:00Z"),
                        null,
                        "dependency",
                        "HIGH",
                        "token=secret",
                        List.of()),
                "password=redacted"));

        assertThat(result.resultStatus()).isEqualTo("SUCCESS");
        assertThat(result.escalate()).isTrue();
    }
}

package com.monagent.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.monagent.analysis.IncidentCandidate;
import com.monagent.analysis.IncidentEvidence;
import com.monagent.analysis.Recommendation;
import com.monagent.analysis.RecommendationActionType;
import com.monagent.analysis.RecommendationRiskLevel;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationDispatcherTest {

    @Test
    void dispatchesSlackAndEmailWithGroundedIncidentContent() {
        NotificationTemplateRenderer renderer = new NotificationTemplateRenderer();
        NotificationDispatcher dispatcher = new NotificationDispatcher(
                List.of(new SlackNotificationChannel(renderer), new EmailNotificationChannel(renderer, notificationProperties())),
                renderer);

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
                "MEDIUM",
                "cpu saturation",
                List.of(new IncidentEvidence(UUID.randomUUID(), UUID.randomUUID(), "ANOMALY", "cpu", "GREATER_THAN", "cpu 91", Instant.parse("2026-07-22T10:05:00Z"), "ref", Map.of())));
        Recommendation recommendation = new Recommendation(
                UUID.randomUUID(),
                incident.incidentId(),
                RecommendationActionType.RESTART_SERVICE,
                "Restart the affected service after confirming current state.",
                RecommendationRiskLevel.HIGH,
                true,
                "PENDING",
                "evidence summary",
                List.of("e-1"),
                Instant.now());

        List<NotificationDeliveryResult> results = dispatcher.dispatch(incident, List.of(recommendation), List.of("slack", "email"));

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(NotificationDeliveryResult::delivered);
        assertThat(results.getFirst().payload()).contains("*Incident*");
        assertThat(results.getLast().payload()).contains("From:");
    }

    @Test
    void reportsUnsupportedChannels() {
        NotificationTemplateRenderer renderer = new NotificationTemplateRenderer();
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(new SlackNotificationChannel(renderer)), renderer);
        IncidentCandidate incident = new IncidentCandidate(
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
                List.of());

        List<NotificationDeliveryResult> results = dispatcher.dispatch(incident, List.of(), List.of("teams"));

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().delivered()).isFalse();
        assertThat(results.getFirst().errorMessage()).contains("Unsupported");
    }

    private com.monagent.config.IntegrationProperties notificationProperties() {
        return new com.monagent.config.IntegrationProperties(
                new com.monagent.config.IntegrationProperties.Database("jdbc:postgresql://localhost:5432/monagent", "monagent", "monagent", "public"),
                new com.monagent.config.IntegrationProperties.Ollama(java.net.URI.create("http://localhost:11434"), "llama3.1:8b-instruct", 30),
                new com.monagent.config.IntegrationProperties.Notifications(List.of("slack", "email"), "monagent@example.com"),
                new com.monagent.config.IntegrationProperties.Auth("issuer", "audience", "roles"),
                new com.monagent.config.IntegrationProperties.Observability("metrics", "traces"));
    }
}

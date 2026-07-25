package com.monagent.notification;

import com.monagent.analysis.IncidentCandidate;
import com.monagent.analysis.Recommendation;
import com.monagent.audit.AuditService;
import com.monagent.web.SelfObservabilityMetrics;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class NotificationDispatcher {

    private final Map<String, NotificationChannel> channels;
    private final NotificationTemplateRenderer renderer;
    private final SelfObservabilityMetrics metrics;
    private final AuditService auditService;

    public NotificationDispatcher(List<NotificationChannel> channels,
                                  NotificationTemplateRenderer renderer,
                                  SelfObservabilityMetrics metrics,
                                  AuditService auditService) {
        this.channels = channels.stream().collect(Collectors.toMap(NotificationChannel::channelName, Function.identity()));
        this.renderer = renderer;
        this.metrics = metrics;
        this.auditService = auditService;
    }

    public List<NotificationDeliveryResult> dispatch(IncidentCandidate incident, List<Recommendation> recommendations, List<String> enabledChannels) {
        NotificationMessage message = buildMessage(incident, recommendations);
        List<NotificationDeliveryResult> results = new ArrayList<>();
        for (String channelName : new LinkedHashSet<>(enabledChannels)) {
            NotificationChannel channel = channels.get(channelName);
            if (channel == null) {
                results.add(new NotificationDeliveryResult(false, channelName, null, "Unsupported channel"));
                metrics.incrementNotificationDelivery(channelName, false);
                auditFailure(incident.incidentId(), channelName, "unsupported channel");
                continue;
            }
            NotificationDeliveryResult result = retry(channel, message);
            metrics.incrementNotificationDelivery(channelName, result.delivered());
            results.add(result);
            if (result.delivered()) {
                auditSuccess(incident.incidentId(), channelName, result.payload());
            } else {
                auditFailure(incident.incidentId(), channelName, result.errorMessage());
            }
        }
        return results;
    }

    private NotificationDeliveryResult retry(NotificationChannel channel, NotificationMessage message) {
        int attempts = 0;
        NotificationDeliveryResult result = null;
        Duration backoff = Duration.ofMillis(100);
        while (attempts < 3) {
            attempts++;
            result = channel.send(message);
            if (result.delivered()) {
                return result;
            }
            sleep(backoff);
            backoff = backoff.multipliedBy(2);
        }
        return result == null ? new NotificationDeliveryResult(false, channel.channelName(), null, "Delivery failed") : result;
    }

    private void auditSuccess(UUID incidentId, String channelName, String payload) {
        auditService.record("system", "NOTIFICATION_DELIVERED", channelName, incidentId, payload == null ? "" : payload);
    }

    private void auditFailure(UUID incidentId, String channelName, String reason) {
        auditService.record("system", "NOTIFICATION_FAILED", channelName, incidentId, reason == null ? "" : reason);
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(Math.max(1L, duration.toMillis()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private NotificationMessage buildMessage(IncidentCandidate incident, List<Recommendation> recommendations) {
        List<String> evidence = incident.evidence().stream().map(item -> item.evidenceId().toString()).toList();
        List<String> nextSteps = recommendations.stream().map(Recommendation::description).toList();
        String symptoms = incident.summary();
        return new NotificationMessage(
                incident.incidentId(),
                incident.severity(),
                incident.affectedServices(),
                symptoms,
                incident.likelyRootCause(),
                incident.confidence(),
                evidence,
                nextSteps,
                renderer.renderMarkdown(new NotificationMessage(
                        incident.incidentId(),
                        incident.severity(),
                        incident.affectedServices(),
                        symptoms,
                        incident.likelyRootCause(),
                        incident.confidence(),
                        evidence,
                        nextSteps,
                        "")));
    }
}

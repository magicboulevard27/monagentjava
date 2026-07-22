package com.monagent.notification;

import com.monagent.analysis.IncidentCandidate;
import com.monagent.analysis.Recommendation;
import java.util.ArrayList;
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

    public NotificationDispatcher(List<NotificationChannel> channels, NotificationTemplateRenderer renderer) {
        this.channels = channels.stream().collect(Collectors.toMap(NotificationChannel::channelName, Function.identity()));
        this.renderer = renderer;
    }

    public List<NotificationDeliveryResult> dispatch(IncidentCandidate incident, List<Recommendation> recommendations, List<String> enabledChannels) {
        NotificationMessage message = buildMessage(incident, recommendations);
        List<NotificationDeliveryResult> results = new ArrayList<>();
        for (String channelName : enabledChannels) {
            NotificationChannel channel = channels.get(channelName);
            if (channel == null) {
                results.add(new NotificationDeliveryResult(false, channelName, null, "Unsupported channel"));
                continue;
            }
            results.add(retry(channel, message));
        }
        return results;
    }

    private NotificationDeliveryResult retry(NotificationChannel channel, NotificationMessage message) {
        int attempts = 0;
        NotificationDeliveryResult result = null;
        while (attempts < 3) {
            attempts++;
            result = channel.send(message);
            if (result.delivered()) {
                return result;
            }
        }
        return result == null ? new NotificationDeliveryResult(false, channel.channelName(), null, "Delivery failed") : result;
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

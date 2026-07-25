package com.monagent.notification;

import org.springframework.stereotype.Component;

@Component
public class IncidentManagementNotificationChannel implements NotificationChannel {

    private final NotificationTemplateRenderer renderer;

    public IncidentManagementNotificationChannel(NotificationTemplateRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public String channelName() {
        return "pagerduty";
    }

    @Override
    public NotificationDeliveryResult send(NotificationMessage message) {
        String payload = """
                Incident management event
                %s
                """.formatted(renderer.renderPlainText(message));
        return new NotificationDeliveryResult(true, channelName(), payload, null);
    }
}

package com.monagent.notification;

import com.monagent.config.IntegrationProperties;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationChannel implements NotificationChannel {

    private final NotificationTemplateRenderer renderer;
    private final IntegrationProperties integrationProperties;

    public EmailNotificationChannel(NotificationTemplateRenderer renderer,
                                    IntegrationProperties integrationProperties) {
        this.renderer = renderer;
        this.integrationProperties = integrationProperties;
    }

    @Override
    public String channelName() {
        return "email";
    }

    @Override
    public NotificationDeliveryResult send(NotificationMessage message) {
        String payload = "From: %s%nSubject: Incident %s [%s]%n%n%s".formatted(
                integrationProperties.notifications().fromAddress(),
                message.incidentId(),
                message.severity(),
                renderer.renderPlainText(message));
        return new NotificationDeliveryResult(true, channelName(), payload, null);
    }
}

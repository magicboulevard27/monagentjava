package com.monagent.notification;

import org.springframework.stereotype.Component;

@Component
public class JiraTicketNotificationChannel implements NotificationChannel {

    private final NotificationTemplateRenderer renderer;

    public JiraTicketNotificationChannel(NotificationTemplateRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public String channelName() {
        return "jira";
    }

    @Override
    public NotificationDeliveryResult send(NotificationMessage message) {
        String payload = """
                Jira ticket created
                %s
                """.formatted(renderer.renderPlainText(message));
        return new NotificationDeliveryResult(true, channelName(), payload, null);
    }
}

package com.monagent.notification;

import org.springframework.stereotype.Component;

@Component
public class SlackNotificationChannel implements NotificationChannel {

    private final NotificationTemplateRenderer renderer;

    public SlackNotificationChannel(NotificationTemplateRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public String channelName() {
        return "slack";
    }

    @Override
    public NotificationDeliveryResult send(NotificationMessage message) {
        return new NotificationDeliveryResult(true, channelName(), renderer.renderMarkdown(message), null);
    }
}

package com.monagent.notification;

import org.springframework.stereotype.Component;

@Component
public class MicrosoftTeamsNotificationChannel implements NotificationChannel {

    private final NotificationTemplateRenderer renderer;

    public MicrosoftTeamsNotificationChannel(NotificationTemplateRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public String channelName() {
        return "teams";
    }

    @Override
    public NotificationDeliveryResult send(NotificationMessage message) {
        return new NotificationDeliveryResult(true, channelName(), renderer.renderMarkdown(message), null);
    }
}

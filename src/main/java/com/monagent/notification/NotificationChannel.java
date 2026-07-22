package com.monagent.notification;

public interface NotificationChannel {

    String channelName();

    NotificationDeliveryResult send(NotificationMessage message);
}

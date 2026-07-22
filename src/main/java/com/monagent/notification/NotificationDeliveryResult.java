package com.monagent.notification;

public record NotificationDeliveryResult(boolean delivered, String channel, String payload, String errorMessage) {
}

package com.monagent.notification;

import java.util.List;
import java.util.UUID;

public record NotificationMessage(
        UUID incidentId,
        String severity,
        List<String> services,
        String symptoms,
        String rootCause,
        String confidence,
        List<String> evidence,
        List<String> nextSteps,
        String markdownBody) {
}

package com.monagent.collection.kubernetes;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class KubernetesEventDetector {

    public String detect(String eventType, String message) {
        String normalizedType = eventType == null ? "" : eventType.toLowerCase(Locale.ROOT);
        String normalizedMessage = message == null ? "" : message.toLowerCase(Locale.ROOT);

        if (normalizedType.contains("crashloop") || normalizedMessage.contains("crashloop")) {
            return "crashloopbackoff";
        }
        if (normalizedType.contains("warning") || normalizedMessage.contains("warning")) {
            return "warning";
        }
        if (normalizedType.contains("failed") || normalizedMessage.contains("failed")) {
            return "failed";
        }
        if (normalizedType.contains("scheduled") || normalizedMessage.contains("deployment")) {
            return "deployment";
        }
        return "event";
    }
}

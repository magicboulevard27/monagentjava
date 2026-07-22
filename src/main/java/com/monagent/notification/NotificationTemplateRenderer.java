package com.monagent.notification;

import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class NotificationTemplateRenderer {

    public String renderMarkdown(NotificationMessage message) {
        return """
                *Incident*: `%s`
                *Severity*: `%s`
                *Services*: %s
                *Symptoms*: %s
                *Root cause*: %s
                *Confidence*: %s
                *Evidence*: %s
                *Next steps*:
                %s
                """.formatted(
                safe(message.incidentId().toString()),
                safe(message.severity()),
                join(message.services()),
                safe(message.symptoms()),
                safe(message.rootCause()),
                safe(message.confidence()),
                join(message.evidence()),
                bulletList(message.nextSteps()));
    }

    public String renderPlainText(NotificationMessage message) {
        return """
                Incident: %s
                Severity: %s
                Services: %s
                Symptoms: %s
                Root cause: %s
                Confidence: %s
                Evidence: %s
                Next steps:
                %s
                """.formatted(
                safe(message.incidentId().toString()),
                safe(message.severity()),
                join(message.services()),
                safe(message.symptoms()),
                safe(message.rootCause()),
                safe(message.confidence()),
                join(message.evidence()),
                bulletList(message.nextSteps()));
    }

    private String join(java.util.List<String> values) {
        if (values == null || values.isEmpty()) {
            return "none";
        }
        return values.stream().map(this::safe).collect(Collectors.joining(", "));
    }

    private String bulletList(java.util.List<String> values) {
        if (values == null || values.isEmpty()) {
            return "- none";
        }
        return values.stream().map(value -> "- " + safe(value)).collect(Collectors.joining(System.lineSeparator()));
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("`", "'")
                .replaceAll("(?i)(password|secret|token|api[-_ ]?key)=[^\\s,;]+", "$1=[REDACTED]")
                .replaceAll("(?i)bearer\\s+[a-z0-9._-]+", "Bearer [REDACTED]");
    }
}

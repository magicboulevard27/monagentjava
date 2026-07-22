package com.monagent.collection.kubernetes;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class KubernetesRedactor {

    private static final Pattern SECRET = Pattern.compile("(?i)(token|secret|password|clientSecret)=([^\\s,;]+)");
    private static final Pattern BEARER = Pattern.compile("(?i)bearer\\s+[A-Za-z0-9-_=]{20,}");
    private static final Pattern EMAIL = Pattern.compile("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}");

    public String redact(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String redacted = SECRET.matcher(input).replaceAll("$1=[REDACTED]");
        redacted = BEARER.matcher(redacted).replaceAll("[REDACTED_BEARER]");
        redacted = EMAIL.matcher(redacted).replaceAll("[REDACTED_EMAIL]");
        return redacted;
    }
}

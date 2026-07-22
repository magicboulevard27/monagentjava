package com.monagent.collection.traces;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TraceRedactor {

    private static final Pattern SECRET = Pattern.compile("(?i)(token|secret|password|apikey|api_key)=([^\\s,;]+)");
    private static final Pattern EMAIL = Pattern.compile("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}");
    private static final Pattern CONNECTION = Pattern.compile("(?i)(jdbc:[^\\s]+|postgres(?:ql)?://[^\\s]+)");

    public String redact(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String redacted = SECRET.matcher(input).replaceAll("$1=[REDACTED]");
        redacted = EMAIL.matcher(redacted).replaceAll("[REDACTED_EMAIL]");
        redacted = CONNECTION.matcher(redacted).replaceAll("[REDACTED_CONNECTION]");
        return redacted;
    }
}

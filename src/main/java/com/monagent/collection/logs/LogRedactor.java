package com.monagent.collection.logs;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class LogRedactor {

    private static final Pattern TOKEN = Pattern.compile("(?i)(bearer\\s+)?[A-Za-z0-9-_=]{20,}");
    private static final Pattern PASSWORD = Pattern.compile("(?i)(password|passwd|pwd)=([^\\s&;]+)");
    private static final Pattern EMAIL = Pattern.compile("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}");
    private static final Pattern CONNECTION_STRING = Pattern.compile("(?i)(jdbc:[^\\s]+|postgres(?:ql)?://[^\\s]+)");
    private static final Pattern COOKIE = Pattern.compile("(?i)(sessionid|cookie)=([^\\s;]+)");

    public String redact(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        return CONNECTION_STRING.matcher(
                COOKIE.matcher(
                        EMAIL.matcher(
                                PASSWORD.matcher(
                                        TOKEN.matcher(input).replaceAll("[REDACTED_TOKEN]"))
                                .replaceAll("$1=[REDACTED_PASSWORD]"))
                        .replaceAll("[REDACTED_EMAIL]"))
                .replaceAll("[REDACTED_CONNECTION]"))
                .replaceAll("$1=[REDACTED_COOKIE]");
    }
}

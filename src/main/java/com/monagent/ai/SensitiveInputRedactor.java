package com.monagent.ai;

import org.springframework.stereotype.Component;

@Component
public class SensitiveInputRedactor {

    public String redact(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replaceAll("(?i)(password|secret|token|api[-_ ]?key)=[^\\s,;]+", "$1=[REDACTED]")
                .replaceAll("(?i)bearer\\s+[a-z0-9._-]+", "Bearer [REDACTED]");
    }
}

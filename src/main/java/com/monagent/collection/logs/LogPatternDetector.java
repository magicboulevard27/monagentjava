package com.monagent.collection.logs;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class LogPatternDetector {

    private static final List<String> PATTERNS = List.of(
            "exception",
            "timeout",
            "connection refused",
            "database",
            "authentication failed",
            "retry exhausted",
            "circuit breaker open",
            "outofmemoryerror");

    public String detect(String message) {
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);
        return PATTERNS.stream()
                .filter(normalized::contains)
                .findFirst()
                .orElse("log.event");
    }
}

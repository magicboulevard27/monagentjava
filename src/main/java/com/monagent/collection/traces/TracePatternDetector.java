package com.monagent.collection.traces;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class TracePatternDetector {

    private static final List<String> PATTERNS = List.of(
            "slow request path",
            "high latency span",
            "failed downstream dependency",
            "service-to-service bottleneck",
            "external api delay");

    public String detect(String summary, long durationMillis, String status, String dependencyName) {
        String normalized = summary == null ? "" : summary.toLowerCase(Locale.ROOT);
        if (durationMillis > 2000) {
            return "high latency span";
        }
        if (status != null && status.equalsIgnoreCase("error")) {
            return "failed downstream dependency";
        }
        if (dependencyName != null && !dependencyName.isBlank() && normalized.contains(dependencyName.toLowerCase(Locale.ROOT))) {
            return "service-to-service bottleneck";
        }
        return PATTERNS.stream().filter(normalized::contains).findFirst().orElse("trace.event");
    }
}

package com.monagent.collection.health;

import java.time.Duration;
import java.util.Map;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class HealthCollectorClient {

    private final WebClient webClient;

    public HealthCollectorClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @CircuitBreaker(name = "healthCollector", fallbackMethod = "fetchHealthFallback")
    @Bulkhead(name = "healthCollector", fallbackMethod = "fetchHealthFallback")
    public Map<String, Object> fetchHealth(String healthUrl, Duration timeout) {
        return fetchJson(healthUrl, timeout);
    }

    @CircuitBreaker(name = "healthCollector", fallbackMethod = "fetchInfoFallback")
    @Bulkhead(name = "healthCollector", fallbackMethod = "fetchInfoFallback")
    public Map<String, Object> fetchInfo(String healthUrl, Duration timeout) {
        return fetchJson(resolveInfoUrl(healthUrl), timeout);
    }

    private Map<String, Object> fetchJson(String url, Duration timeout) {
        return webClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block(timeout);
    }

    private Map<String, Object> fetchHealthFallback(String healthUrl, Duration timeout, Throwable throwable) {
        return Map.of("status", "UNKNOWN");
    }

    private Map<String, Object> fetchInfoFallback(String healthUrl, Duration timeout, Throwable throwable) {
        return Map.of();
    }

    private String resolveInfoUrl(String healthUrl) {
        if (healthUrl == null || healthUrl.isBlank()) {
            return "/actuator/info";
        }
        return healthUrl.endsWith("/health")
                ? healthUrl.substring(0, healthUrl.length() - "/health".length()) + "/info"
                : (healthUrl.endsWith("/") ? healthUrl + "info" : healthUrl + "/info");
    }
}

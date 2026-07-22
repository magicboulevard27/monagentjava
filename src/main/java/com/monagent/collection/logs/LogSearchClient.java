package com.monagent.collection.logs;

import java.time.Duration;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class LogSearchClient {

    private final WebClient webClient;

    public LogSearchClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> query(String endpoint, String serviceName, String environment, String severity, Duration timeout) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(endpoint)
                        .queryParam("service", serviceName)
                        .queryParam("environment", environment)
                        .queryParam("severity", severity)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block(timeout);
    }
}

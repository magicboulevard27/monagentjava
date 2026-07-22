package com.monagent.collection.traces;

import java.time.Duration;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class TraceSearchClient {

    private final WebClient webClient;

    public TraceSearchClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> query(String endpoint, String serviceName, String operation, String status, Duration timeout) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(endpoint)
                        .queryParam("service", serviceName)
                        .queryParam("operation", operation)
                        .queryParam("status", status)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block(timeout);
    }
}

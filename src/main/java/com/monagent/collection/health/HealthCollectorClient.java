package com.monagent.collection.health;

import java.time.Duration;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class HealthCollectorClient {

    private final WebClient webClient;

    public HealthCollectorClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Map<String, Object> fetchHealth(String healthUrl, Duration timeout) {
        return webClient.get()
                .uri(healthUrl)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block(timeout);
    }
}

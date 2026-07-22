package com.monagent.collection.kubernetes;

import java.time.Duration;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class KubernetesEventClient {

    private final WebClient webClient;

    public KubernetesEventClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> query(String endpoint, String namespace, String workload, Duration timeout) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(endpoint)
                        .queryParam("namespace", namespace)
                        .queryParam("workload", workload)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block(timeout);
    }
}

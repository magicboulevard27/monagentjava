package com.monagent.collection.prometheus;

import java.time.Duration;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PrometheusQueryClient {

    private final WebClient webClient;

    public PrometheusQueryClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> query(String endpoint, String promql, Duration timeout) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path(endpoint).queryParam("query", promql).build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block(timeout);
    }
}

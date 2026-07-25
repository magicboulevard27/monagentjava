package com.monagent.collection.prometheus;

import java.time.Duration;
import java.util.Map;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PrometheusQueryClient {

    private final WebClient webClient;
    private final PrometheusCollectorProperties properties;

    public PrometheusQueryClient(WebClient webClient, PrometheusCollectorProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> query(String endpoint, String promql, Duration timeout) {
        return webClient.get()
                .uri(UriComponentsBuilder.fromUri(properties.baseUrl())
                        .path(endpoint)
                        .queryParam("query", promql)
                        .build(true)
                        .toUri())
                .headers(this::applyAuthentication)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block(timeout);
    }

    private void applyAuthentication(org.springframework.http.HttpHeaders headers) {
        if (properties.bearerToken() != null && !properties.bearerToken().isBlank()) {
            headers.setBearerAuth(properties.bearerToken());
            return;
        }
        if (properties.basicAuthUsername() != null && properties.basicAuthPassword() != null) {
            headers.setBasicAuth(properties.basicAuthUsername(), properties.basicAuthPassword());
        }
    }
}

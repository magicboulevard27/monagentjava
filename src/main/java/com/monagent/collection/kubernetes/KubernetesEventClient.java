package com.monagent.collection.kubernetes;

import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

public class KubernetesEventClient {

    private final WebClient webClient;
    private final KubernetesCollectorProperties properties;

    public KubernetesEventClient(WebClient webClient, KubernetesCollectorProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> query(String endpoint, String namespace, String workload, Duration timeout) {
        return webClient.get()
                .uri(UriComponentsBuilder.fromUri(properties.resolvedBaseUrl())
                        .path(endpoint)
                        .queryParam("namespace", namespace)
                        .queryParam("workload", workload)
                        .build()
                        .toUri())
                .headers(this::applyAuthentication)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block(timeout);
    }

    private void applyAuthentication(HttpHeaders headers) {
        if (properties.bearerToken() != null && !properties.bearerToken().isBlank()) {
            headers.setBearerAuth(properties.bearerToken());
        }
    }
}

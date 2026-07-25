package com.monagent.collection.logs;

import java.time.Duration;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class LogSearchClient {

    private final WebClient webClient;
    private final LogAnalyzerProperties properties;

    public LogSearchClient(WebClient webClient, LogAnalyzerProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> query(String endpoint, String serviceName, String environment, String severity, Duration timeout) {
        return webClient.get()
                .uri(UriComponentsBuilder.fromUri(properties.baseUrl())
                        .path(endpoint)
                        .queryParam("service", serviceName)
                        .queryParam("environment", environment)
                        .queryParam("severity", severity)
                        .build(true)
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
            return;
        }
        if (properties.basicAuthUsername() != null && properties.basicAuthPassword() != null) {
            headers.setBasicAuth(properties.basicAuthUsername(), properties.basicAuthPassword());
        }
    }
}

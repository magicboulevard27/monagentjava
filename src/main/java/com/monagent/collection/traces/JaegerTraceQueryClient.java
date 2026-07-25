package com.monagent.collection.traces;

import java.util.Map;

import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Primary
public class JaegerTraceQueryClient implements TraceQueryClient {

    private final WebClient webClient;
    private final TraceAnalyzerProperties properties;

    public JaegerTraceQueryClient(WebClient webClient, TraceAnalyzerProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    @Override
    public TraceQueryResult query(TraceQuery query) {
        Map<String, Object> payload = webClient.get()
                .uri(UriComponentsBuilder.fromUriString(properties.endpoint())
                        .queryParam("service", query.serviceName())
                        .queryParam("operation", query.operation())
                        .queryParam("status", query.status())
                        .build()
                        .toUri())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block(query.timeout());
        return new TraceQueryResult(payload);
    }
}

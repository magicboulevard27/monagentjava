package com.monagent.collection.traces;

import java.util.Map;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

public class TraceSearchClient implements TraceQueryClient {

    private final WebClient webClient;
    private final TraceAnalyzerProperties properties;

    public TraceSearchClient(WebClient webClient, TraceAnalyzerProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    @Retry(name = "traceSearch")
    @CircuitBreaker(name = "traceSearch", fallbackMethod = "queryFallback")
    @Bulkhead(name = "traceSearch", fallbackMethod = "queryFallback")
    @SuppressWarnings("unchecked")
    @Override
    public TraceQueryResult query(TraceQuery query) {
        Map<String, Object> payload = webClient.get()
                .uri(UriComponentsBuilder.fromUriString(properties.endpoint())
                        .queryParam("service", query.serviceName())
                        .queryParam("operation", query.operation())
                        .queryParam("status", query.status())
                        .queryParam("start", query.incidentWindowStart().toString())
                        .queryParam("end", query.incidentWindowEnd().toString())
                        .build()
                        .toUri())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block(query.timeout());
        return new TraceQueryResult(payload);
    }

    private TraceQueryResult queryFallback(TraceQuery query, Throwable throwable) {
        return new TraceQueryResult(Map.of("spans", java.util.List.of(), "queryStatus", "unavailable"));
    }
}

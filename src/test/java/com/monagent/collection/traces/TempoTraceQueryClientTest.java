package com.monagent.collection.traces;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class TempoTraceQueryClientTest {

    @Test
    void queriesTempoStyleEndpointThroughNeutralInterface() {
        AtomicReference<ClientRequest> requestRef = new AtomicReference<>();
        ExchangeFunction exchangeFunction = request -> {
            requestRef.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"data\":{\"durationMillis\":3200}}")
                    .build());
        };
        TraceAnalyzerProperties properties = new TraceAnalyzerProperties(
                "https://tempo.internal/api/traces",
                Duration.ofSeconds(5),
                15,
                3,
                50,
                2);
        TempoTraceQueryClient client = new TempoTraceQueryClient(WebClient.builder().exchangeFunction(exchangeFunction).build(), properties);

        TraceQueryResult result = client.query(new TraceQuery(
                "orders",
                "GET /checkout",
                "error",
                Instant.parse("2026-07-24T06:45:00Z"),
                Instant.parse("2026-07-24T07:00:00Z"),
                Duration.ofSeconds(5)));

        assertThat(result.payload()).containsKey("data");
        assertThat(requestRef.get().url().toString()).contains("https://tempo.internal/api/traces");
        assertThat(requestRef.get().url().getQuery()).contains("start=2026-07-24T06:45:00Z");
        assertThat(requestRef.get().url().getQuery()).contains("end=2026-07-24T07:00:00Z");
    }
}

package com.monagent.collection.traces;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
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

class TraceSearchClientTest {

    @Test
    void queriesUsingProviderNeutralTraceRequest() {
        AtomicReference<ClientRequest> requestRef = new AtomicReference<>();
        ExchangeFunction exchangeFunction = request -> {
            requestRef.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"data\":{\"durationMillis\":250}}")
                    .build());
        };
        TraceAnalyzerProperties properties = new TraceAnalyzerProperties(
                "https://jaeger.internal/api/traces",
                Duration.ofSeconds(5),
                15);
        TraceSearchClient client = new TraceSearchClient(WebClient.builder().exchangeFunction(exchangeFunction).build(), properties);

        TraceQueryResult result = client.query(new TraceQuery(
                "orders",
                "GET /checkout",
                "error",
                Instant.parse("2026-07-24T06:45:00Z"),
                Instant.parse("2026-07-24T07:00:00Z"),
                Duration.ofSeconds(5)));

        assertThat(result.payload()).containsKey("data");
        ClientRequest request = requestRef.get();
        assertThat(request.url().toString()).contains("https://jaeger.internal/api/traces");
        assertThat(request.url().getQuery()).contains("service=orders");
        assertThat(request.url().getQuery()).contains("operation=GET /checkout");
        assertThat(request.url().getQuery()).contains("status=error");
        assertThat(request.url().getQuery()).contains("start=2026-07-24T06:45:00Z");
        assertThat(request.url().getQuery()).contains("end=2026-07-24T07:00:00Z");
    }
}

package com.monagent.collection.logs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.List;
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

class LogSearchClientTest {

    @Test
    void appliesBearerTokenAndQueryParameters() {
        AtomicReference<ClientRequest> requestRef = new AtomicReference<>();
        ExchangeFunction exchangeFunction = request -> {
            requestRef.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"hits\":[]}")
                    .build());
        };
        LogAnalyzerProperties properties = new LogAnalyzerProperties(
                URI.create("https://opensearch.internal:9200"),
                "/_search",
                Duration.ofSeconds(5),
                10,
                3,
                50,
                2,
                "token-123",
                null,
                null);
        LogSearchClient client = new LogSearchClient(WebClient.builder().exchangeFunction(exchangeFunction).build(), properties);

        Map<String, Object> response = client.query(properties.endpoint(), "orders", "prod", "error", Duration.ofSeconds(5));

        assertThat(response).containsKey("hits");
        ClientRequest request = requestRef.get();
        assertThat(request.url().toString()).contains("https://opensearch.internal:9200/_search");
        assertThat(request.url().getQuery()).contains("service=orders");
        assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token-123");
    }

    @Test
    void appliesBasicAuthWhenConfigured() {
        AtomicReference<ClientRequest> requestRef = new AtomicReference<>();
        ExchangeFunction exchangeFunction = request -> {
            requestRef.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"hits\":[]}")
                    .build());
        };
        LogAnalyzerProperties properties = new LogAnalyzerProperties(
                URI.create("https://opensearch.internal:9200"),
                "/_search",
                Duration.ofSeconds(5),
                10,
                3,
                50,
                2,
                null,
                "readonly",
                "secret");
        LogSearchClient client = new LogSearchClient(WebClient.builder().exchangeFunction(exchangeFunction).build(), properties);

        client.query(properties.endpoint(), "orders", "prod", "error", Duration.ofSeconds(5));

        assertThat(requestRef.get().headers().getFirst(HttpHeaders.AUTHORIZATION)).startsWith("Basic ");
    }
}

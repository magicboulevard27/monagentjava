package com.monagent.collection.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
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

class KubernetesEventClientTest {

    @Test
    void usesLocalBaseUrlAndNoBearerToken() {
        AtomicReference<ClientRequest> requestRef = new AtomicReference<>();
        ExchangeFunction exchangeFunction = request -> {
            requestRef.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"eventType\":\"Warning\"}")
                    .build());
        };
        KubernetesCollectorProperties properties = new KubernetesCollectorProperties(
                "/apis/monitoring.monagent.io/v1/events",
                Duration.ofSeconds(5),
                15,
                false,
                null,
                null,
                null);
        KubernetesEventClient client = new KubernetesEventClient(WebClient.builder().exchangeFunction(exchangeFunction).build(), properties);

        Map<String, Object> response = client.query(properties.endpoint(), "default", "orders", properties.timeout());

        assertThat(response).containsEntry("eventType", "Warning");
        assertThat(requestRef.get().url().toString()).startsWith("http://localhost:8001");
        assertThat(requestRef.get().headers().getFirst(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @Test
    void appliesBearerTokenInCluster() {
        AtomicReference<ClientRequest> requestRef = new AtomicReference<>();
        ExchangeFunction exchangeFunction = request -> {
            requestRef.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"eventType\":\"Warning\"}")
                    .build());
        };
        KubernetesCollectorProperties properties = new KubernetesCollectorProperties(
                "/apis/monitoring.monagent.io/v1/events",
                Duration.ofSeconds(5),
                15,
                true,
                URI.create("https://kubernetes.default.svc"),
                "token-123",
                "/var/run/secrets/kubernetes.io/serviceaccount/token");
        KubernetesEventClient client = new KubernetesEventClient(WebClient.builder().exchangeFunction(exchangeFunction).build(), properties);

        client.query(properties.endpoint(), "default", "orders", properties.timeout());

        assertThat(requestRef.get().url().toString()).startsWith("https://kubernetes.default.svc");
        assertThat(requestRef.get().headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token-123");
    }
}

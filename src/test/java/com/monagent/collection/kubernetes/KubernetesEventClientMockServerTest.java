package com.monagent.collection.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

class KubernetesEventClientMockServerTest {

    @Test
    void queriesMockKubernetesApiServer() throws Exception {
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> requestAuth = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/apis/monitoring.monagent.io/v1/events", exchange -> respond(exchange, requestPath, requestAuth));
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        try {
            KubernetesCollectorProperties properties = new KubernetesCollectorProperties(
                    "/apis/monitoring.monagent.io/v1/events",
                    Duration.ofSeconds(5),
                    15,
                    false,
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                    null,
                    null);
            KubernetesEventClient client = new KubernetesEventClient(WebClient.builder().build(), properties);

            Map<String, Object> response = client.query(properties.endpoint(), "default", "orders", properties.timeout());

            assertThat(response).containsEntry("eventType", "Warning");
            assertThat(requestPath.get()).contains("/apis/monitoring.monagent.io/v1/events");
            assertThat(requestPath.get()).contains("namespace=default");
            assertThat(requestPath.get()).contains("workload=orders");
            assertThat(requestAuth.get()).isNull();
        } finally {
            server.stop(0);
        }
    }

    private void respond(HttpExchange exchange, AtomicReference<String> requestPath, AtomicReference<String> requestAuth) throws IOException {
        requestPath.set(exchange.getRequestURI().toString());
        requestAuth.set(exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        byte[] body = "{\"eventType\":\"Warning\",\"resourceKind\":\"Pod\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }
}

package com.monagent.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.monagent.ai.OllamaIncidentAnalysisClient;
import com.monagent.collection.kubernetes.KubernetesCollectorProperties;
import com.monagent.collection.kubernetes.KubernetesEventClient;
import com.monagent.collection.logs.LogAnalyzerProperties;
import com.monagent.collection.logs.LogSearchClient;
import com.monagent.collection.prometheus.PrometheusCollectorProperties;
import com.monagent.collection.prometheus.PrometheusQueryClient;
import com.monagent.collection.traces.JaegerTraceQueryClient;
import com.monagent.collection.traces.TempoTraceQueryClient;
import com.monagent.collection.traces.TraceAnalyzerProperties;
import com.monagent.collection.traces.TraceQuery;
import com.monagent.collection.traces.TraceSearchClient;
import com.monagent.config.IntegrationProperties;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class ExternalIntegrationFaultInjectionTest {

    @Test
    void logSearchClientFailsFastOnDownstreamError() {
        LogSearchClient client = new LogSearchClient(WebClient.builder().exchangeFunction(failingExchange()).build(), logProperties());

        assertThatThrownBy(() -> client.query("/_search", "orders", "prod", "error", Duration.ofSeconds(1)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void traceClientsFailFastOnDownstreamError() {
        TraceAnalyzerProperties properties = traceProperties();
        TraceQuery query = new TraceQuery(
                "orders",
                "GET /checkout",
                "error",
                Instant.parse("2026-07-24T06:45:00Z"),
                Instant.parse("2026-07-24T07:00:00Z"),
                Duration.ofSeconds(1));

        assertThatThrownBy(() -> new TraceSearchClient(WebClient.builder().exchangeFunction(failingExchange()).build(), properties).query(query))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> new JaegerTraceQueryClient(WebClient.builder().exchangeFunction(failingExchange()).build(), properties).query(query))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> new TempoTraceQueryClient(WebClient.builder().exchangeFunction(failingExchange()).build(), properties).query(query))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void kubernetesClientFailsFastOnDownstreamError() {
        KubernetesEventClient client = new KubernetesEventClient(WebClient.builder().exchangeFunction(failingExchange()).build(), kubernetesProperties());

        assertThatThrownBy(() -> client.query("/apis/monitoring.monagent.io/v1/events", "default", "orders", Duration.ofSeconds(1)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void prometheusClientFailsFastOnDownstreamError() {
        PrometheusQueryClient client = new PrometheusQueryClient(WebClient.builder().exchangeFunction(failingExchange()).build(), prometheusProperties());

        assertThatThrownBy(() -> client.query("/api/v1/query", "cpu", Duration.ofSeconds(1)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void ollamaClientFailsAfterRetryOnDownstreamError() {
        IntegrationProperties integrationProperties = integrationProperties();
        OllamaIncidentAnalysisClient client = new OllamaIncidentAnalysisClient(WebClient.builder(), integrationProperties);

        assertThatThrownBy(() -> client.analyze("summarize this incident"))
                .isInstanceOf(RuntimeException.class);
    }

    private ExchangeFunction failingExchange() {
        return request -> Mono.error(new RuntimeException("downstream unavailable"));
    }

    private LogAnalyzerProperties logProperties() {
        return new LogAnalyzerProperties(
                URI.create("https://opensearch.internal:9200"),
                "/_search",
                Duration.ofSeconds(1),
                15,
                3,
                50,
                2,
                null,
                null,
                null);
    }

    private TraceAnalyzerProperties traceProperties() {
        return new TraceAnalyzerProperties(
                "https://jaeger.internal/api/traces",
                Duration.ofSeconds(1),
                15,
                3,
                50,
                2);
    }

    private KubernetesCollectorProperties kubernetesProperties() {
        return new KubernetesCollectorProperties(
                "/apis/monitoring.monagent.io/v1/events",
                Duration.ofSeconds(1),
                15,
                false,
                URI.create("https://kubernetes.default.svc"),
                null,
                null);
    }

    private PrometheusCollectorProperties prometheusProperties() {
        return new PrometheusCollectorProperties(
                Duration.ofSeconds(1),
                URI.create("https://prometheus.internal:9090"),
                null,
                null,
                null,
                List.of(new PrometheusCollectorProperties.MetricQuery("cpu", "cpu", "cores")));
    }

    private IntegrationProperties integrationProperties() {
        return new IntegrationProperties(
                new IntegrationProperties.Database("jdbc:postgresql://localhost:5432/monagent", "monagent", "monagent", "public"),
                new IntegrationProperties.Ollama(URI.create("http://localhost:11434"), "llama3.1:8b-instruct", 1, 2, 50, 2),
                new IntegrationProperties.Notifications(List.of("slack"), "monagent@example.com"),
                new IntegrationProperties.Auth("https://login.microsoftonline.com/common/v2.0", "api://monagentjava", "roles"),
                new IntegrationProperties.Observability("http://localhost:9090", "http://localhost:4317"));
    }
}

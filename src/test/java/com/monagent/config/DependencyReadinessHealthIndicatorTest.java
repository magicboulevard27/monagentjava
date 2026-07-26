package com.monagent.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.monagent.collection.logs.LogAnalyzerProperties;
import com.monagent.collection.prometheus.PrometheusCollectorProperties;
import com.monagent.collection.traces.TraceAnalyzerProperties;
import java.net.URI;
import java.sql.Connection;
import java.time.Duration;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;

class DependencyReadinessHealthIndicatorTest {

    @Test
    void reportsReadyWhenMandatoryDatabaseIsAvailableAndOptionalDependenciesAreConfigured() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(1)).thenReturn(true);

        IntegrationProperties integrationProperties = new IntegrationProperties(
                new IntegrationProperties.Database("jdbc:postgresql://localhost:5432/monagent", "monagent", "monagent", "public"),
                new IntegrationProperties.Ollama(URI.create("http://localhost:11434"), "llama3.1:8b-instruct", 30, 3, 50, 2),
                new IntegrationProperties.Notifications(List.of("slack"), "monagent@example.com"),
                new IntegrationProperties.Auth("https://login.microsoftonline.com/common/v2.0", "api://monagentjava", "roles"),
                new IntegrationProperties.Observability("http://localhost:9090", "http://localhost:4317"));
        LogAnalyzerProperties logProperties = new LogAnalyzerProperties(
                URI.create("https://opensearch.internal:9200"),
                "/_search",
                Duration.ofSeconds(5),
                15,
                3,
                50,
                2,
                null,
                null,
                null);
        TraceAnalyzerProperties traceProperties = new TraceAnalyzerProperties(
                "https://jaeger.internal/api/traces",
                Duration.ofSeconds(5),
                15,
                3,
                50,
                2);
        PrometheusCollectorProperties prometheusProperties = new PrometheusCollectorProperties(
                Duration.ofSeconds(5),
                URI.create("http://localhost:9090"),
                null,
                null,
                null,
                List.of(new PrometheusCollectorProperties.MetricQuery("cpu", "avg(rate(process_cpu_seconds_total[5m]))", "cores")));

        DependencyReadinessHealthIndicator indicator = new DependencyReadinessHealthIndicator(
                dataSource, integrationProperties, logProperties, traceProperties, prometheusProperties);

        Health health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("UP");
        assertThat(health.getDetails()).containsEntry("mandatory.database", "UP");
        assertThat(health.getDetails().toString()).contains("optional.dependencies");
    }

    @Test
    void reportsDownWhenMandatoryDatabaseIsUnavailable() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(1)).thenReturn(false);

        IntegrationProperties integrationProperties = new IntegrationProperties(
                new IntegrationProperties.Database("jdbc:postgresql://localhost:5432/monagent", "monagent", "monagent", "public"),
                new IntegrationProperties.Ollama(URI.create("http://localhost:11434"), "llama3.1:8b-instruct", 30, 3, 50, 2),
                new IntegrationProperties.Notifications(List.of("slack"), "monagent@example.com"),
                new IntegrationProperties.Auth("https://login.microsoftonline.com/common/v2.0", "api://monagentjava", "roles"),
                new IntegrationProperties.Observability("http://localhost:9090", "http://localhost:4317"));
        LogAnalyzerProperties logProperties = new LogAnalyzerProperties(
                URI.create("https://opensearch.internal:9200"),
                "/_search",
                Duration.ofSeconds(5),
                15,
                3,
                50,
                2,
                null,
                null,
                null);
        TraceAnalyzerProperties traceProperties = new TraceAnalyzerProperties(
                "https://jaeger.internal/api/traces",
                Duration.ofSeconds(5),
                15,
                3,
                50,
                2);
        PrometheusCollectorProperties prometheusProperties = new PrometheusCollectorProperties(
                Duration.ofSeconds(5),
                URI.create("http://localhost:9090"),
                null,
                null,
                null,
                List.of(new PrometheusCollectorProperties.MetricQuery("cpu", "avg(rate(process_cpu_seconds_total[5m]))", "cores")));

        DependencyReadinessHealthIndicator indicator = new DependencyReadinessHealthIndicator(
                dataSource, integrationProperties, logProperties, traceProperties, prometheusProperties);

        Health health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
        assertThat(health.getDetails()).containsEntry("mandatory.database", "DOWN");
    }
}

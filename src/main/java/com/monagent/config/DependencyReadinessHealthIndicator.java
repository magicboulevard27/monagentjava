package com.monagent.config;

import com.monagent.collection.logs.LogAnalyzerProperties;
import com.monagent.collection.prometheus.PrometheusCollectorProperties;
import com.monagent.collection.traces.TraceAnalyzerProperties;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class DependencyReadinessHealthIndicator implements HealthIndicator {

    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(1);

    private final DataSource dataSource;
    private final IntegrationProperties integrationProperties;
    private final LogAnalyzerProperties logAnalyzerProperties;
    private final TraceAnalyzerProperties traceAnalyzerProperties;
    private final PrometheusCollectorProperties prometheusCollectorProperties;

    public DependencyReadinessHealthIndicator(
            DataSource dataSource,
            IntegrationProperties integrationProperties,
            LogAnalyzerProperties logAnalyzerProperties,
            TraceAnalyzerProperties traceAnalyzerProperties,
            PrometheusCollectorProperties prometheusCollectorProperties) {
        this.dataSource = dataSource;
        this.integrationProperties = integrationProperties;
        this.logAnalyzerProperties = logAnalyzerProperties;
        this.traceAnalyzerProperties = traceAnalyzerProperties;
        this.prometheusCollectorProperties = prometheusCollectorProperties;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();

        Health database = checkDatabase();
        details.put("mandatory.database", database.getStatus().getCode());

        Map<String, String> optionalStatuses = new LinkedHashMap<>();
        optionalStatuses.put("logs", optionalStatus(logAnalyzerProperties.endpoint()));
        optionalStatuses.put("traces", optionalStatus(traceAnalyzerProperties.endpoint()));
        optionalStatuses.put("prometheus", optionalStatus(prometheusCollectorProperties.baseUrl()));
        optionalStatuses.put("ollama", optionalStatus(integrationProperties.ollama().baseUrl()));
        details.put("optional.dependencies", optionalStatuses);

        if (!database.getStatus().getCode().equals("UP")) {
            return Health.down().withDetails(details).build();
        }
        return Health.up().withDetails(details).build();
    }

    private Health checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid((int) CONNECTION_TIMEOUT.toSeconds())) {
                return Health.up().build();
            }
            return Health.down().withDetail("error", "database connection is not valid").build();
        } catch (SQLException ex) {
            return Health.down().withException(ex).build();
        }
    }

    private String optionalStatus(Object endpoint) {
        if (endpoint == null) {
            return "not-configured";
        }
        String value = endpoint.toString().trim();
        return value.isBlank() ? "not-configured" : "optional";
    }
}

package com.monagent.collection.prometheus;

import com.monagent.api.service.MonitoredServiceService;
import com.monagent.collection.MonitoringSignalPersistenceService;
import com.monagent.collection.SignalNormalizationService;
import com.monagent.collection.model.NormalizedSignal;
import com.monagent.domain.MonitoredService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PrometheusMetricsCollectorService {

    private static final Logger log = LoggerFactory.getLogger(PrometheusMetricsCollectorService.class);

    private static final String PROMETHEUS_API = "/api/v1/query";

    private final MonitoredServiceService monitoredServiceService;
    private final PrometheusCollectorProperties properties;
    private final PrometheusQueryClient client;
    private final MonitoringSignalPersistenceService persistenceService;
    private final SignalNormalizationService normalizationService;

    public PrometheusMetricsCollectorService(
            MonitoredServiceService monitoredServiceService,
            PrometheusCollectorProperties properties,
            PrometheusQueryClient client,
            MonitoringSignalPersistenceService persistenceService,
            SignalNormalizationService normalizationService) {
        this.monitoredServiceService = monitoredServiceService;
        this.properties = properties;
        this.client = client;
        this.persistenceService = persistenceService;
        this.normalizationService = normalizationService;
    }

    @Scheduled(fixedDelayString = "${monagent.collectors.prometheus.interval-seconds:60}000")
    public void collect() {
        List<NormalizedSignal> batch = new ArrayList<>();
        for (MonitoredService service : monitoredServiceService.list()) {
            if (!service.enabled()) {
                continue;
            }
            batch.addAll(collect(service));
        }
        persistenceService.saveAll(batch);
    }

    public List<NormalizedSignal> collect(MonitoredService service) {
        return properties.queries().stream().flatMap(query -> {
            var response = client.query(PROMETHEUS_API, query.promql(), properties.timeout());
            return PrometheusMetricMapper.parseInstantVector(service.serviceName(), service.environment(), response, query.name(), query.unit())
                    .stream()
                    .map(PrometheusMetricMapper::toSourceSignal)
                    .map(normalizationService::fromMetrics)
                    .map(persistenceService::save);
        }).toList();
    }
}

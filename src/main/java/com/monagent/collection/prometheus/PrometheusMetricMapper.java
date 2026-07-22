package com.monagent.collection.prometheus;

import com.monagent.collection.model.MetricsSourceSignal;
import com.monagent.collection.model.NormalizedSignal;
import com.monagent.collection.SignalNormalizationService;
import java.time.Instant;
import java.util.List;
import java.util.Map;

final class PrometheusMetricMapper {

    private PrometheusMetricMapper() {
    }

    static MetricsSourceSignal toSourceSignal(PrometheusMetricSample sample) {
        return new MetricsSourceSignal(
                sample.serviceId(),
                sample.serviceName(),
                sample.environment(),
                sample.observedAt(),
                sample.metricName(),
                sample.value(),
                sample.unit(),
                sample.rawReference());
    }

    static List<PrometheusMetricSample> parseInstantVector(String serviceName, String environment, Map<String, Object> response, String metricName, String unit) {
        Object data = response.get("data");
        if (!(data instanceof Map<?, ?> dataMap)) {
            return List.of();
        }
        Object result = dataMap.get("result");
        if (!(result instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(item -> {
            Map<?, ?> sample = (Map<?, ?>) item;
            Map<?, ?> value = (Map<?, ?>) sample.get("value");
            double metricValue = Double.parseDouble(String.valueOf(value.get("1")));
            return new PrometheusMetricSample(
                    java.util.UUID.randomUUID(),
                    serviceName,
                    environment,
                    Instant.parse(String.valueOf(value.get("0"))),
                    metricName,
                    metricValue,
                    unit,
                    String.valueOf(sample));
        }).toList();
    }
}

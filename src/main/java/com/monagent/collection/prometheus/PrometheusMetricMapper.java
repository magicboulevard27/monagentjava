package com.monagent.collection.prometheus;

import com.monagent.collection.model.MetricsSourceSignal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

final class PrometheusMetricMapper {

    private PrometheusMetricMapper() {
    }

    static MetricsSourceSignal toSourceSignal(PrometheusMetricSample sample) {
        return new MetricsSourceSignal(
                sample.serviceId(),
                sample.serviceName(),
                sample.environment(),
                normalizeTimestamp(sample.observedAt()),
                normalizeMetricName(sample.metricName()),
                normalizeValue(sample.value()),
                normalizeUnit(sample.unit()),
                normalizeRawReference(sample.rawReference(), sample.metricName(), sample.unit(), sample.observedAt(), sample.value()));
    }

    static List<PrometheusMetricSample> parseInstantVector(String serviceName, String environment, Map<String, Object> response, String metricName, String unit) {
        if (!isSuccessfulVector(response)) {
            return List.of();
        }
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
            Map<?, ?> value = extractValue(sample);
            double metricValue = parseMetricValue(value.get("1"));
            Instant observedAt = parseObservedAt(value.get("0"));
            return new PrometheusMetricSample(
                    java.util.UUID.randomUUID(),
                    serviceName,
                    environment,
                    observedAt,
                    metricName,
                    metricValue,
                    normalizeUnit(unit),
                    String.valueOf(normalizeLabels(sample)));
        }).toList();
    }

    private static boolean isSuccessfulVector(Map<String, Object> response) {
        Object status = response.get("status");
        if (status != null && !"success".equalsIgnoreCase(String.valueOf(status))) {
            return false;
        }
        Object data = response.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object resultType = dataMap.get("resultType");
            return resultType == null || "vector".equalsIgnoreCase(String.valueOf(resultType));
        }
        return false;
    }

    private static Map<?, ?> extractValue(Map<?, ?> sample) {
        Object value = sample.get("value");
        if (value instanceof Map<?, ?> valueMap) {
            return valueMap;
        }
        return Map.of();
    }

    private static double parseMetricValue(Object rawValue) {
        try {
            double value = Double.parseDouble(String.valueOf(rawValue));
            return Double.isFinite(value) ? value : 0d;
        } catch (RuntimeException ex) {
            return 0d;
        }
    }

    private static Instant parseObservedAt(Object rawObservedAt) {
        try {
            return Instant.parse(String.valueOf(rawObservedAt));
        } catch (DateTimeParseException ex) {
            return Instant.EPOCH;
        }
    }

    private static String normalizeMetricName(String metricName) {
        return metricName == null ? "unknown" : metricName.trim().toLowerCase(Locale.ROOT).replace(' ', '.');
    }

    private static String normalizeUnit(String unit) {
        if (unit == null) {
            return "";
        }
        return switch (unit.trim().toLowerCase(Locale.ROOT)) {
            case "bytes", "byte", "b" -> "bytes";
            case "cores", "core" -> "cores";
            case "percent", "%", "percentage" -> "percent";
            case "seconds", "s" -> "seconds";
            case "milliseconds", "ms" -> "ms";
            case "requests-per-second", "rps" -> "rps";
            default -> unit.trim().toLowerCase(Locale.ROOT);
        };
    }

    private static Instant normalizeTimestamp(Instant observedAt) {
        return observedAt == null ? Instant.EPOCH : observedAt;
    }

    private static double normalizeValue(double value) {
        return Double.isFinite(value) ? value : 0d;
    }

    private static Map<String, Object> normalizeLabels(Map<?, ?> sample) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : sample.entrySet()) {
            String key = String.valueOf(entry.getKey()).trim().toLowerCase(Locale.ROOT).replace(' ', '_');
            if ("value".equals(key)) {
                continue;
            }
            if ("metric".equals(key) && entry.getValue() instanceof Map<?, ?> metricLabels) {
                for (Map.Entry<?, ?> label : metricLabels.entrySet()) {
                    String labelKey = String.valueOf(label.getKey()).trim().toLowerCase(Locale.ROOT).replace(' ', '_');
                    normalized.put(labelKey, label.getValue());
                }
                continue;
            }
            normalized.put(key, entry.getValue());
        }
        return normalized;
    }

    private static String normalizeRawReference(String rawReference, String metricName, String unit, Instant observedAt, double value) {
        return Map.of(
                "metricName", normalizeMetricName(metricName),
                "unit", normalizeUnit(unit),
                "observedAt", normalizeTimestamp(observedAt).toString(),
                "value", normalizeValue(value),
                "raw", rawReference == null ? "" : rawReference)
                .toString();
    }
}

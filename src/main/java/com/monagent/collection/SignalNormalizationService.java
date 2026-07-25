package com.monagent.collection;

import com.monagent.collection.model.CiCdSourceSignal;
import com.monagent.collection.model.HealthSourceSignal;
import com.monagent.collection.model.KubernetesSourceSignal;
import com.monagent.collection.model.LogSourceSignal;
import com.monagent.collection.model.MetricsSourceSignal;
import com.monagent.collection.model.NormalizedSignal;
import com.monagent.collection.model.SignalSeverity;
import com.monagent.collection.model.SignalStatus;
import com.monagent.collection.model.SourceType;
import com.monagent.collection.model.TraceSourceSignal;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SignalNormalizationService {

    public NormalizedSignal fromHealth(HealthSourceSignal source) {
        requireSource(source, "health");
        SignalStatus status = switch (normalize(source.healthState())) {
            case "up" -> SignalStatus.UP;
            case "down" -> SignalStatus.DOWN;
            case "degraded" -> SignalStatus.DEGRADED;
            default -> SignalStatus.UNKNOWN;
        };
        SignalSeverity severity = switch (status) {
            case DOWN -> SignalSeverity.CRITICAL;
            case DEGRADED -> SignalSeverity.HIGH;
            case UP, UNKNOWN, OK, WARN, ERROR -> SignalSeverity.NONE;
        };
        return base(source.serviceId(), SourceType.HEALTH, "service.health", source.observedAt(),
                source.healthState(), null, status, severity, source.rawReference());
    }

    public NormalizedSignal fromMetrics(MetricsSourceSignal source) {
        requireSource(source, "metrics");
        double value = Double.isFinite(source.value()) ? source.value() : 0d;
        SignalStatus status = value > 0 ? SignalStatus.OK : SignalStatus.UNKNOWN;
        SignalSeverity severity = switch (normalize(source.metricName())) {
            case "cpu" -> value > 80 ? SignalSeverity.HIGH : SignalSeverity.NONE;
            case "memory" -> value > 85 ? SignalSeverity.HIGH : SignalSeverity.NONE;
            default -> SignalSeverity.NONE;
        };
        return base(source.serviceId(), SourceType.METRICS, source.metricName(), source.observedAt(),
                Double.toString(value), source.unit(), status, severity, source.rawReference());
    }

    public NormalizedSignal fromLog(LogSourceSignal source) {
        requireSource(source, "log");
        SignalSeverity severity = switch (normalize(source.pattern())) {
            case "exception", "timeout", "connectionrefused", "databaseerror", "authenticationfailure",
                 "retryexhaustion", "circuitbreakeropen", "outofmemoryerror" -> SignalSeverity.HIGH;
            default -> SignalSeverity.MEDIUM;
        };
        return base(source.serviceId(), SourceType.LOGS, source.pattern(), source.observedAt(),
                safeSummary(source.message()), null, SignalStatus.WARN, severity, source.rawReference());
    }

    public NormalizedSignal fromTrace(TraceSourceSignal source) {
        requireSource(source, "trace");
        long durationMillis = Math.max(0L, source.durationMillis());
        SignalSeverity severity = durationMillis > 2000 ? SignalSeverity.HIGH : SignalSeverity.MEDIUM;
        return base(source.serviceId(), SourceType.TRACES, source.spanName(), source.observedAt(),
                Long.toString(durationMillis), "ms", SignalStatus.WARN, severity, source.rawReference());
    }

    public NormalizedSignal fromKubernetes(KubernetesSourceSignal source) {
        requireSource(source, "kubernetes");
        SignalStatus status = switch (normalize(source.eventType())) {
            case "failed", "crashloopbackoff", "unhealthy" -> SignalStatus.DOWN;
            case "warning" -> SignalStatus.DEGRADED;
            default -> SignalStatus.UNKNOWN;
        };
        SignalSeverity severity = status == SignalStatus.DOWN ? SignalSeverity.HIGH : SignalSeverity.MEDIUM;
        return base(source.serviceId(), SourceType.KUBERNETES, source.resourceKind(), source.observedAt(),
                safeSummary(source.message()), null, status, severity, source.rawReference());
    }

    public NormalizedSignal fromCiCd(CiCdSourceSignal source) {
        requireSource(source, "cicd");
        String name = "deployment." + normalize(source.changeType());
        return base(source.serviceId(), SourceType.CICD, name, source.observedAt(),
                source.revision(), null, SignalStatus.OK, SignalSeverity.NONE, source.rawReference());
    }

    private NormalizedSignal base(UUID serviceId, SourceType sourceType, String signalName, Instant observedAt,
            String value, String unit, SignalStatus status, SignalSeverity severity, String rawReference) {
        String normalizedName = normalizeName(signalName);
        String normalizedValue = value == null ? "" : value;
        return new NormalizedSignal(
                UUID.nameUUIDFromBytes((serviceId + "|" + sourceType + "|" + normalizedName + "|" + observedAt + "|"
                        + normalizedValue + "|" + (unit == null ? "" : unit) + "|" + status + "|" + severity + "|"
                        + (rawReference == null ? "" : rawReference)).getBytes(StandardCharsets.UTF_8)),
                serviceId,
                sourceType,
                normalizedName,
                normalizedValue,
                unit,
                status,
                severity,
                observedAt,
                rawReference);
    }

    private String normalizeName(String value) {
        String normalized = normalize(value).replace(' ', '.');
        return normalized.isBlank() ? "unknown" : normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safeSummary(String message) {
        if (message == null) {
            return "";
        }
        return message.length() > 240 ? message.substring(0, 240) : message;
    }

    private void requireSource(Object source, String type) {
        if (source == null) {
            throw new IllegalArgumentException("Missing " + type + " source signal");
        }
    }
}

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
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SignalNormalizationService {

    public NormalizedSignal fromHealth(HealthSourceSignal source) {
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
        SignalStatus status = source.value() > 0 ? SignalStatus.OK : SignalStatus.UNKNOWN;
        SignalSeverity severity = switch (normalize(source.metricName())) {
            case "cpu" -> source.value() > 80 ? SignalSeverity.HIGH : SignalSeverity.NONE;
            case "memory" -> source.value() > 85 ? SignalSeverity.HIGH : SignalSeverity.NONE;
            default -> SignalSeverity.NONE;
        };
        return base(source.serviceId(), SourceType.METRICS, source.metricName(), source.observedAt(),
                Double.toString(source.value()), source.unit(), status, severity, source.rawReference());
    }

    public NormalizedSignal fromLog(LogSourceSignal source) {
        SignalSeverity severity = switch (normalize(source.pattern())) {
            case "exception", "timeout", "connectionrefused", "databaseerror", "authenticationfailure",
                 "retryexhaustion", "circuitbreakeropen", "outofmemoryerror" -> SignalSeverity.HIGH;
            default -> SignalSeverity.MEDIUM;
        };
        return base(source.serviceId(), SourceType.LOGS, source.pattern(), source.observedAt(),
                safeSummary(source.message()), null, SignalStatus.WARN, severity, source.rawReference());
    }

    public NormalizedSignal fromTrace(TraceSourceSignal source) {
        SignalSeverity severity = source.durationMillis() > 2000 ? SignalSeverity.HIGH : SignalSeverity.MEDIUM;
        return base(source.serviceId(), SourceType.TRACES, source.spanName(), source.observedAt(),
                Long.toString(source.durationMillis()), "ms", SignalStatus.WARN, severity, source.rawReference());
    }

    public NormalizedSignal fromKubernetes(KubernetesSourceSignal source) {
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
        String name = "deployment." + normalize(source.changeType());
        return base(source.serviceId(), SourceType.CICD, name, source.observedAt(),
                source.revision(), null, SignalStatus.OK, SignalSeverity.NONE, source.rawReference());
    }

    private NormalizedSignal base(UUID serviceId, SourceType sourceType, String signalName, Instant observedAt,
            String value, String unit, SignalStatus status, SignalSeverity severity, String rawReference) {
        return new NormalizedSignal(
                UUID.randomUUID(),
                serviceId,
                sourceType,
                normalizeName(signalName),
                value,
                unit,
                status,
                severity,
                observedAt,
                rawReference);
    }

    private String normalizeName(String value) {
        return normalize(value).replace(' ', '.');
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
}

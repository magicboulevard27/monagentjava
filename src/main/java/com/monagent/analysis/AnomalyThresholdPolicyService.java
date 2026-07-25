package com.monagent.analysis;

import com.monagent.api.service.MonitoredServiceService;
import com.monagent.collection.model.NormalizedSignal;
import com.monagent.domain.MonitoredService;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AnomalyThresholdPolicyService {

    private static final ThresholdPolicy DEFAULT_POLICY = new ThresholdPolicy(
            "unknown",
            BigDecimal.ZERO,
            ThresholdComparator.GREATER_THAN,
            5,
            1,
            "LOW",
            "UNKNOWN",
            0,
            10);

    private final AnomalyPolicyProperties properties;
    private final MonitoredServiceService monitoredServiceService;

    public AnomalyThresholdPolicyService(AnomalyPolicyProperties properties, MonitoredServiceService monitoredServiceService) {
        this.properties = properties;
        this.monitoredServiceService = monitoredServiceService;
    }

    public ThresholdPolicy resolve(NormalizedSignal signal) {
        Optional<MonitoredService> service = findService(signal);
        return properties.rules().stream()
                .filter(rule -> matches(rule, signal, service.orElse(null)))
                .sorted(Comparator.comparingInt(this::specificity).reversed())
                .findFirst()
                .map(this::toPolicy)
                .orElse(defaultPolicy(signal));
    }

    private Optional<MonitoredService> findService(NormalizedSignal signal) {
        try {
            return Optional.of(monitoredServiceService.get(signal.serviceId()));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private boolean matches(AnomalyPolicyProperties.Rule rule, NormalizedSignal signal, MonitoredService service) {
        return fieldMatches(rule.serviceName(), service == null ? null : service.serviceName())
                && fieldMatches(rule.environment(), service == null ? null : service.environment())
                && fieldMatches(rule.metricName(), signal.signalName())
                && fieldMatches(rule.severity(), severityName(signal));
    }

    private boolean fieldMatches(String expected, String actual) {
        return expected == null || expected.isBlank() || (actual != null && expected.equalsIgnoreCase(actual));
    }

    private int specificity(AnomalyPolicyProperties.Rule rule) {
        int score = 0;
        if (rule.serviceName() != null && !rule.serviceName().isBlank()) {
            score++;
        }
        if (rule.environment() != null && !rule.environment().isBlank()) {
            score++;
        }
        if (rule.metricName() != null && !rule.metricName().isBlank()) {
            score++;
        }
        if (rule.severity() != null && !rule.severity().isBlank()) {
            score++;
        }
        return score;
    }

    private ThresholdPolicy toPolicy(AnomalyPolicyProperties.Rule rule) {
        return new ThresholdPolicy(
                rule.metricName(),
                rule.thresholdValue(),
                rule.comparator(),
                rule.evaluationWindowMinutes(),
                rule.minimumSampleSize(),
                rule.detectedSeverity(),
                rule.outcomeStatus(),
                rule.cooldownMinutes(),
                rule.hysteresisPercent());
    }

    private ThresholdPolicy defaultPolicy(NormalizedSignal signal) {
        return new ThresholdPolicy(
                signal.signalName(),
                DEFAULT_POLICY.thresholdValue(),
                DEFAULT_POLICY.comparator(),
                DEFAULT_POLICY.evaluationWindowMinutes(),
                DEFAULT_POLICY.minimumSampleSize(),
                DEFAULT_POLICY.severity(),
                DEFAULT_POLICY.outcomeStatus(),
                DEFAULT_POLICY.cooldownMinutes(),
                DEFAULT_POLICY.hysteresisPercent());
    }

    private String severityName(NormalizedSignal signal) {
        return signal.severity() == null ? "" : signal.severity().name();
    }
}

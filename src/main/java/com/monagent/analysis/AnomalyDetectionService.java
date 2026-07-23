package com.monagent.analysis;

import com.monagent.collection.model.NormalizedSignal;
import com.monagent.collection.model.SignalSeverity;
import com.monagent.collection.model.SignalStatus;
import com.monagent.collection.model.SourceType;
import com.monagent.persistence.AnomalyOutcomeEntity;
import com.monagent.persistence.AnomalyOutcomeRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionService.class);

    private final AnomalyOutcomeRepository anomalyOutcomeRepository;

    public AnomalyDetectionService(AnomalyOutcomeRepository anomalyOutcomeRepository) {
        this.anomalyOutcomeRepository = anomalyOutcomeRepository;
    }

    public AnomalyOutcome evaluate(NormalizedSignal signal) {
        ThresholdPolicy policy = policyFor(signal);
        BigDecimal observedValue = parseObservedValue(signal.signalValue());
        boolean triggered = triggered(policy, observedValue, signal);
        Instant detectedAt = signal.collectedAt();
        Instant cooldownUntil = triggered ? detectedAt.plus(Duration.ofMinutes(policy.cooldownMinutes())) : null;

        AnomalyOutcome outcome = new AnomalyOutcome(
                UUID.randomUUID(),
                signal.serviceId(),
                signal.signalId(),
                policy.metricName(),
                policy.thresholdValue(),
                observedValue,
                policy.comparator(),
                policy.severity(),
                triggered ? "TRIGGERED" : "SUPPRESSED",
                policy.evaluationWindowMinutes(),
                policy.minimumSampleSize(),
                detectedAt,
                cooldownUntil,
                List.of(signal.rawReference()));

        persist(outcome);
        return outcome;
    }

    private void persist(AnomalyOutcome outcome) {
        AnomalyOutcomeEntity entity = new AnomalyOutcomeEntity();
        entity.setAnomalyId(outcome.anomalyId());
        entity.setServiceId(outcome.serviceId());
        entity.setSignalId(outcome.signalId());
        entity.setMetricName(outcome.metricName());
        entity.setThresholdValue(outcome.thresholdValue());
        entity.setObservedValue(outcome.observedValue());
        entity.setComparator(outcome.comparator().name());
        entity.setSeverity(outcome.severity());
        entity.setOutcomeStatus(outcome.outcomeStatus());
        entity.setEvaluationWindowMinutes(outcome.evaluationWindowMinutes());
        entity.setMinimumSampleSize(outcome.minimumSampleSize());
        entity.setDetectedAt(outcome.detectedAt());
        entity.setCooldownUntil(outcome.cooldownUntil());
        entity.setSupportingReferences(outcome.supportingReferences().toString());
        anomalyOutcomeRepository.saveAndFlush(entity);
    }

    private ThresholdPolicy policyFor(NormalizedSignal signal) {
        String metric = signal.signalName().toLowerCase();
        return switch (metric) {
            case "service.health" -> new ThresholdPolicy(metric, BigDecimal.ZERO, ThresholdComparator.EQUALS, 5, 1, "CRITICAL", "DOWN", 0);
            case "cpu" -> new ThresholdPolicy(metric, new BigDecimal("80"), ThresholdComparator.GREATER_THAN, 5, 3, "HIGH", "OVER_THRESHOLD", 10);
            case "memory" -> new ThresholdPolicy(metric, new BigDecimal("85"), ThresholdComparator.GREATER_THAN, 5, 3, "HIGH", "OVER_THRESHOLD", 10);
            case "request.rate" -> new ThresholdPolicy(metric, BigDecimal.ZERO, ThresholdComparator.GREATER_THAN, 5, 3, "MEDIUM", "SPIKE", 5);
            case "error.rate" -> new ThresholdPolicy(metric, new BigDecimal("5"), ThresholdComparator.GREATER_THAN, 5, 3, "HIGH", "OVER_THRESHOLD", 10);
            case "request.latency" -> new ThresholdPolicy(metric, new BigDecimal("2"), ThresholdComparator.GREATER_THAN, 5, 3, "HIGH", "OVER_THRESHOLD", 10);
            case "db.pool" -> new ThresholdPolicy(metric, new BigDecimal("90"), ThresholdComparator.GREATER_THAN, 5, 3, "HIGH", "OVER_THRESHOLD", 10);
            case "kafka.lag" -> new ThresholdPolicy(metric, BigDecimal.ZERO, ThresholdComparator.INCREASING, 5, 3, "HIGH", "INCREASING", 10);
            default -> new ThresholdPolicy(metric, BigDecimal.ZERO, ThresholdComparator.GREATER_THAN, 5, 1, "LOW", "UNKNOWN", 0);
        };
    }

    private boolean triggered(ThresholdPolicy policy, BigDecimal observedValue, NormalizedSignal signal) {
        if (signal.sourceType() == SourceType.HEALTH) {
            return signal.status() == SignalStatus.DOWN;
        }
        return switch (policy.comparator()) {
            case GREATER_THAN -> observedValue.compareTo(policy.thresholdValue()) > 0;
            case GREATER_THAN_OR_EQUAL -> observedValue.compareTo(policy.thresholdValue()) >= 0;
            case LESS_THAN -> observedValue.compareTo(policy.thresholdValue()) < 0;
            case LESS_THAN_OR_EQUAL -> observedValue.compareTo(policy.thresholdValue()) <= 0;
            case EQUALS -> observedValue.compareTo(policy.thresholdValue()) == 0;
            case INCREASING -> signal.status() == SignalStatus.OK && observedValue.compareTo(policy.thresholdValue()) > 0;
        };
    }

    private BigDecimal parseObservedValue(String value) {
        try {
            return new BigDecimal(value.replaceAll("[^0-9.\\-]", ""));
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }
}

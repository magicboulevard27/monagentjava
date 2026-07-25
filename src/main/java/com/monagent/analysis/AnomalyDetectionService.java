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
    private final AnomalyThresholdPolicyService thresholdPolicyService;

    public AnomalyDetectionService(AnomalyOutcomeRepository anomalyOutcomeRepository,
                                   AnomalyThresholdPolicyService thresholdPolicyService) {
        this.anomalyOutcomeRepository = anomalyOutcomeRepository;
        this.thresholdPolicyService = thresholdPolicyService;
    }

    public AnomalyOutcome evaluate(NormalizedSignal signal) {
        ThresholdPolicy policy = thresholdPolicyService.resolve(signal);
        BigDecimal observedValue = parseObservedValue(signal.signalValue());
        if (shouldSuppress(signal, policy, observedValue)) {
            return persistSuppressed(signal, policy, observedValue);
        }
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

    private AnomalyOutcome persistSuppressed(NormalizedSignal signal, ThresholdPolicy policy, BigDecimal observedValue) {
        AnomalyOutcome outcome = new AnomalyOutcome(
                UUID.randomUUID(),
                signal.serviceId(),
                signal.signalId(),
                policy.metricName(),
                policy.thresholdValue(),
                observedValue,
                policy.comparator(),
                policy.severity(),
                "SUPPRESSED",
                policy.evaluationWindowMinutes(),
                policy.minimumSampleSize(),
                signal.collectedAt(),
                null,
                List.of(signal.rawReference()));
        persist(outcome);
        return outcome;
    }

    private boolean shouldSuppress(NormalizedSignal signal, ThresholdPolicy policy, BigDecimal observedValue) {
        return anomalyOutcomeRepository.findTopByServiceIdAndMetricNameOrderByDetectedAtDesc(signal.serviceId(), policy.metricName())
                .filter(previous -> previous.getDetectedAt() != null)
                .map(previous -> isDuplicate(previous, signal, observedValue, policy) || isFlapping(previous, observedValue, policy, signal))
                .orElse(false);
    }

    private boolean isDuplicate(AnomalyOutcomeEntity previous, NormalizedSignal signal, BigDecimal observedValue, ThresholdPolicy policy) {
        return previous.getObservedValue() != null
                && previous.getSignalId() != null
                && previous.getSignalId().equals(signal.signalId())
                && previous.getDetectedAt().plus(Duration.ofMinutes(policy.evaluationWindowMinutes())).isAfter(signal.collectedAt())
                && previous.getObservedValue().compareTo(observedValue) == 0;
    }

    private boolean isFlapping(AnomalyOutcomeEntity previous, BigDecimal observedValue, ThresholdPolicy policy, NormalizedSignal signal) {
        if (previous.getCooldownUntil() == null || !previous.getCooldownUntil().isAfter(signal.collectedAt())) {
            return false;
        }
        return withinHysteresisBand(policy, observedValue);
    }

    private boolean triggered(ThresholdPolicy policy, BigDecimal observedValue, NormalizedSignal signal) {
        if (signal.sourceType() == SourceType.HEALTH) {
            return signal.status() == SignalStatus.DOWN;
        }
        if (policy.comparator() == ThresholdComparator.INCREASING) {
            return increasing(signal, observedValue, policy);
        }
        if (isCoolingDown(signal, policy, observedValue)) {
            return false;
        }
        return switch (policy.comparator()) {
            case GREATER_THAN -> beyondHysteresis(policy, observedValue, true);
            case GREATER_THAN_OR_EQUAL -> beyondHysteresis(policy, observedValue, true);
            case LESS_THAN -> beyondHysteresis(policy, observedValue, false);
            case LESS_THAN_OR_EQUAL -> beyondHysteresis(policy, observedValue, false);
            case EQUALS -> observedValue.compareTo(policy.thresholdValue()) == 0;
            case INCREASING -> false;
        };
    }

    private boolean increasing(NormalizedSignal signal, BigDecimal observedValue, ThresholdPolicy policy) {
        if (signal.status() != SignalStatus.OK || signal.signalName() == null || !"kafka.lag".equalsIgnoreCase(signal.signalName())) {
            return false;
        }
        List<AnomalyOutcomeEntity> samples = anomalyOutcomeRepository.findTop5ByServiceIdAndMetricNameOrderByDetectedAtDesc(signal.serviceId(), policy.metricName());
        if (samples.size() < policy.minimumSampleSize()) {
            return false;
        }
        List<AnomalyOutcomeEntity> ordered = samples.stream()
                .filter(previous -> previous.getDetectedAt() != null && previous.getObservedValue() != null)
                .sorted((left, right) -> left.getDetectedAt().compareTo(right.getDetectedAt()))
                .toList();
        boolean insideWindow = ordered.stream()
                .allMatch(previous -> !previous.getDetectedAt().isBefore(signal.collectedAt().minus(Duration.ofMinutes(policy.evaluationWindowMinutes()))));
        if (!insideWindow) {
            return false;
        }
        return ordered.size() >= policy.minimumSampleSize()
                && isStrictlyIncreasing(ordered)
                && observedValue.compareTo(policy.thresholdValue()) > 0;
    }

    private boolean isStrictlyIncreasing(List<AnomalyOutcomeEntity> samples) {
        BigDecimal previous = null;
        for (AnomalyOutcomeEntity sample : samples) {
            BigDecimal current = sample.getObservedValue();
            if (current == null) {
                return false;
            }
            if (previous != null && current.compareTo(previous) <= 0) {
                return false;
            }
            previous = current;
        }
        return true;
    }

    private boolean isCoolingDown(NormalizedSignal signal, ThresholdPolicy policy, BigDecimal observedValue) {
        return anomalyOutcomeRepository.findTopByServiceIdAndMetricNameOrderByDetectedAtDesc(signal.serviceId(), policy.metricName())
                .filter(previous -> previous.getCooldownUntil() != null
                        && previous.getCooldownUntil().isAfter(signal.collectedAt()))
                .map(previous -> withinHysteresisBand(policy, observedValue))
                .orElse(false);
    }

    private boolean beyondHysteresis(ThresholdPolicy policy, BigDecimal observedValue, boolean above) {
        BigDecimal band = policy.thresholdValue().multiply(BigDecimal.valueOf(policy.hysteresisPercent())).divide(BigDecimal.valueOf(100));
        BigDecimal upper = policy.thresholdValue().add(band);
        BigDecimal lower = policy.thresholdValue().subtract(band);
        return above ? observedValue.compareTo(upper) > 0 : observedValue.compareTo(lower) < 0;
    }

    private boolean withinHysteresisBand(ThresholdPolicy policy, BigDecimal observedValue) {
        BigDecimal band = policy.thresholdValue().multiply(BigDecimal.valueOf(policy.hysteresisPercent())).divide(BigDecimal.valueOf(100));
        BigDecimal upper = policy.thresholdValue().add(band);
        BigDecimal lower = policy.thresholdValue().subtract(band);
        return observedValue.compareTo(lower) >= 0 && observedValue.compareTo(upper) <= 0;
    }

    private BigDecimal parseObservedValue(String value) {
        try {
            return new BigDecimal(value.replaceAll("[^0-9.\\-]", ""));
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }
}

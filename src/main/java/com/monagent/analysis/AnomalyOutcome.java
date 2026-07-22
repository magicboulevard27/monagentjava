package com.monagent.analysis;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnomalyOutcome(
        UUID anomalyId,
        UUID serviceId,
        UUID signalId,
        String metricName,
        BigDecimal thresholdValue,
        BigDecimal observedValue,
        ThresholdComparator comparator,
        String severity,
        String outcomeStatus,
        int evaluationWindowMinutes,
        int minimumSampleSize,
        Instant detectedAt,
        Instant cooldownUntil,
        List<String> supportingReferences) {
}

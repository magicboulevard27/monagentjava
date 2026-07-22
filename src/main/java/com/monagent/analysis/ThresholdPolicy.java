package com.monagent.analysis;

import java.math.BigDecimal;

public record ThresholdPolicy(
        String metricName,
        BigDecimal thresholdValue,
        ThresholdComparator comparator,
        int evaluationWindowMinutes,
        int minimumSampleSize,
        String severity,
        String outcomeStatus,
        int cooldownMinutes) {
}

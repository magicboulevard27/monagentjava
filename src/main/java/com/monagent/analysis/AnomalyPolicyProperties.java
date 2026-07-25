package com.monagent.analysis;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "monagent.anomaly")
public record AnomalyPolicyProperties(
        @NotEmpty @Valid List<Rule> rules) {

    public record Rule(
            String serviceName,
            String environment,
            @NotNull String metricName,
            String severity,
            @NotNull BigDecimal thresholdValue,
            @NotNull ThresholdComparator comparator,
            @NotNull String outcomeStatus,
            @NotNull String detectedSeverity,
            int evaluationWindowMinutes,
            int minimumSampleSize,
            int cooldownMinutes,
            int hysteresisPercent) {
    }
}

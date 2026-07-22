package com.monagent.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "anomaly_outcomes")
public class AnomalyOutcomeEntity {

    @Id
    @Column(name = "anomaly_id", nullable = false, updatable = false)
    private UUID anomalyId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "signal_id")
    private UUID signalId;

    @Column(name = "metric_name", nullable = false, length = 200)
    private String metricName;

    @Column(name = "threshold_value", precision = 19, scale = 4)
    private BigDecimal thresholdValue;

    @Column(name = "observed_value", precision = 19, scale = 4)
    private BigDecimal observedValue;

    @Column(name = "comparator", nullable = false, length = 20)
    private String comparator;

    @Column(name = "severity", nullable = false, length = 50)
    private String severity;

    @Column(name = "outcome_status", nullable = false, length = 50)
    private String outcomeStatus;

    @Column(name = "evaluation_window_minutes", nullable = false)
    private int evaluationWindowMinutes;

    @Column(name = "minimum_sample_size", nullable = false)
    private int minimumSampleSize;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "cooldown_until")
    private Instant cooldownUntil;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "supporting_references", columnDefinition = "jsonb")
    private String supportingReferences;

    protected AnomalyOutcomeEntity() {
    }

    public UUID getAnomalyId() {
        return anomalyId;
    }

    public void setAnomalyId(UUID anomalyId) {
        this.anomalyId = anomalyId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public UUID getSignalId() {
        return signalId;
    }

    public void setSignalId(UUID signalId) {
        this.signalId = signalId;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public BigDecimal getObservedValue() {
        return observedValue;
    }

    public void setObservedValue(BigDecimal observedValue) {
        this.observedValue = observedValue;
    }

    public String getComparator() {
        return comparator;
    }

    public void setComparator(String comparator) {
        this.comparator = comparator;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getOutcomeStatus() {
        return outcomeStatus;
    }

    public void setOutcomeStatus(String outcomeStatus) {
        this.outcomeStatus = outcomeStatus;
    }

    public int getEvaluationWindowMinutes() {
        return evaluationWindowMinutes;
    }

    public void setEvaluationWindowMinutes(int evaluationWindowMinutes) {
        this.evaluationWindowMinutes = evaluationWindowMinutes;
    }

    public int getMinimumSampleSize() {
        return minimumSampleSize;
    }

    public void setMinimumSampleSize(int minimumSampleSize) {
        this.minimumSampleSize = minimumSampleSize;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }

    public Instant getCooldownUntil() {
        return cooldownUntil;
    }

    public void setCooldownUntil(Instant cooldownUntil) {
        this.cooldownUntil = cooldownUntil;
    }

    public String getSupportingReferences() {
        return supportingReferences;
    }

    public void setSupportingReferences(String supportingReferences) {
        this.supportingReferences = supportingReferences;
    }
}

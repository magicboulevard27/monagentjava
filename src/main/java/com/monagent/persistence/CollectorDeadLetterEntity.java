package com.monagent.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "collector_dead_letters")
public class CollectorDeadLetterEntity {

    @Id
    @Column(name = "dead_letter_id", nullable = false, updatable = false)
    private UUID deadLetterId;

    @Column(name = "job_type", nullable = false, length = 50)
    private String jobType;

    @Column(name = "service_id", nullable = false, length = 100)
    private String serviceId;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "payload", nullable = false, length = 1000)
    private String payload;

    @Column(name = "failed_at", nullable = false)
    private Instant failedAt;

    public CollectorDeadLetterEntity() {
    }

    public UUID getDeadLetterId() {
        return deadLetterId;
    }

    public void setDeadLetterId(UUID deadLetterId) {
        this.deadLetterId = deadLetterId;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }
}

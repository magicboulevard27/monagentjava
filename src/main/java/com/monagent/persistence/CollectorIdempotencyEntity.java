package com.monagent.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "collector_idempotency_keys")
public class CollectorIdempotencyEntity {

    @Id
    @Column(name = "key_id", nullable = false, updatable = false)
    private UUID keyId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 200)
    private String idempotencyKey;

    @Column(name = "job_type", nullable = false, length = 50)
    private String jobType;

    @Column(name = "service_id", nullable = false, length = 100)
    private String serviceId;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public CollectorIdempotencyEntity() {
    }

    public UUID getKeyId() {
        return keyId;
    }

    public void setKeyId(UUID keyId) {
        this.keyId = keyId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
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

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}

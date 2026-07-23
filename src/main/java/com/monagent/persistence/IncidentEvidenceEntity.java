package com.monagent.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "incident_evidence")
public class IncidentEvidenceEntity {

    @Id
    @Column(name = "evidence_id", nullable = false, updatable = false)
    private UUID evidenceId;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "service_name", length = 200)
    private String serviceName;

    @Column(name = "evidence_type", nullable = false, length = 100)
    private String evidenceType;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "reference_id", length = 200)
    private String referenceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "redacted_payload", columnDefinition = "jsonb")
    private String redactedPayload;

    public IncidentEvidenceEntity() {
    }

    public UUID getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(UUID evidenceId) {
        this.evidenceId = evidenceId;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(UUID incidentId) {
        this.incidentId = incidentId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(Instant observedAt) {
        this.observedAt = observedAt;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getRedactedPayload() {
        return redactedPayload;
    }

    public void setRedactedPayload(String redactedPayload) {
        this.redactedPayload = redactedPayload;
    }
}

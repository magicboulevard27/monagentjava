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

    protected IncidentEvidenceEntity() {
    }
}

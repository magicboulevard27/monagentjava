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
@Table(name = "incidents")
public class IncidentEntity {

    @Id
    @Column(name = "incident_id", nullable = false, updatable = false)
    private UUID incidentId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "severity", nullable = false, length = 50)
    private String severity;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "affected_services", nullable = false, columnDefinition = "jsonb")
    private String affectedServices;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "likely_root_cause")
    private String likelyRootCause;

    @Column(name = "confidence", length = 50)
    private String confidence;

    @Column(name = "summary")
    private String summary;

    protected IncidentEntity() {
    }
}

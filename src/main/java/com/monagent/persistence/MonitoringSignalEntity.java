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
@Table(name = "monitoring_signals")
public class MonitoringSignalEntity {

    @Id
    @Column(name = "signal_id", nullable = false, updatable = false)
    private UUID signalId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "signal_name", nullable = false, length = 200)
    private String signalName;

    @Column(name = "signal_value", nullable = false, length = 500)
    private String signalValue;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "severity", length = 50)
    private String severity;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_reference", columnDefinition = "jsonb")
    private String rawReference;

    protected MonitoringSignalEntity() {
    }
}

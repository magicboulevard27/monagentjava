package com.monagent.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recommendations")
public class RecommendationEntity {

    @Id
    @Column(name = "recommendation_id", nullable = false, updatable = false)
    private UUID recommendationId;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "action_type", nullable = false, length = 100)
    private String actionType;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "risk_level", nullable = false, length = 50)
    private String riskLevel;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "evidence_summary")
    private String evidenceSummary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RecommendationEntity() {
    }
}

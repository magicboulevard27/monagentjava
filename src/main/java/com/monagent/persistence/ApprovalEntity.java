package com.monagent.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approvals")
public class ApprovalEntity {

    @Id
    @Column(name = "approval_id", nullable = false, updatable = false)
    private UUID approvalId;

    @Column(name = "recommendation_id", nullable = false)
    private UUID recommendationId;

    @Column(name = "requested_by", nullable = false, length = 200)
    private String requestedBy;

    @Column(name = "approved_by", length = 200)
    private String approvedBy;

    @Column(name = "approval_status", nullable = false, length = 50)
    private String approvalStatus;

    @Column(name = "decision_reason")
    private String decisionReason;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ApprovalEntity() {
    }
}

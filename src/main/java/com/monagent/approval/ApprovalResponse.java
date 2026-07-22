package com.monagent.approval;

import java.time.Instant;
import java.util.UUID;

public record ApprovalResponse(
        UUID approvalId,
        UUID recommendationId,
        String requestedBy,
        String approvedBy,
        String approvalStatus,
        String decisionReason,
        Instant decidedAt,
        long version) {
}

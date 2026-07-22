package com.monagent.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
        UUID auditId,
        String actor,
        String action,
        String entityType,
        UUID entityId,
        String eventPayload,
        Instant createdAt) {
}

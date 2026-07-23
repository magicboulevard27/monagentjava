package com.monagent.audit;

import com.monagent.persistence.AuditLogEntity;
import com.monagent.persistence.AuditLogRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditEvent record(String actor, String action, String entityType, UUID entityId, String eventPayload) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setAuditId(UUID.randomUUID());
        entity.setActor(actor);
        entity.setAction(action);
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setEventPayload(eventPayload);
        entity.setCreatedAt(Instant.now());
        auditLogRepository.saveAndFlush(entity);
        return new AuditEvent(entity.getAuditId(), actor, action, entityType, entityId, eventPayload, entity.getCreatedAt());
    }
}

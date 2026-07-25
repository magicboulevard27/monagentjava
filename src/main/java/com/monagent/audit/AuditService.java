package com.monagent.audit;

import com.monagent.persistence.AuditLogEntity;
import com.monagent.persistence.AuditLogRepository;
import com.monagent.security.DataEncryptionService;
import com.monagent.security.RedactionService;
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
    private final RedactionService redactionService;
    private final DataEncryptionService dataEncryptionService;

    public AuditService(AuditLogRepository auditLogRepository, RedactionService redactionService, DataEncryptionService dataEncryptionService) {
        this.auditLogRepository = auditLogRepository;
        this.redactionService = redactionService;
        this.dataEncryptionService = dataEncryptionService;
    }

    @Transactional
    public AuditEvent record(String actor, String action, String entityType, UUID entityId, String eventPayload) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setAuditId(UUID.randomUUID());
        entity.setActor(actor);
        entity.setAction(action);
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setEventPayload(dataEncryptionService.encrypt(redactionService.redact(eventPayload)));
        entity.setCreatedAt(Instant.now());
        auditLogRepository.saveAndFlush(entity);
        return new AuditEvent(entity.getAuditId(), actor, action, entityType, entityId, redactionService.redact(eventPayload), entity.getCreatedAt());
    }
}

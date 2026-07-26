package com.monagent.audit;

import com.monagent.config.RetentionProperties;
import com.monagent.persistence.AuditLogEntity;
import com.monagent.persistence.AuditLogRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditGovernanceService {

    private final AuditLogRepository auditLogRepository;
    private final RetentionProperties retentionProperties;

    public AuditGovernanceService(AuditLogRepository auditLogRepository, RetentionProperties retentionProperties) {
        this.auditLogRepository = auditLogRepository;
        this.retentionProperties = retentionProperties;
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> exportSince(Instant since) {
        return auditLogRepository.findAllByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(since)
                .stream()
                .map(this::toEvent)
                .toList();
    }

    @Transactional(readOnly = true)
    public Instant retentionCutoff() {
        Duration retention = retentionProperties.auditLogs();
        return retention == null ? Instant.EPOCH : Instant.now().minus(retention);
    }

    @Transactional(readOnly = true)
    public boolean verifyIntegrity() {
        String previousHash = null;
        for (AuditLogEntity entity : auditLogRepository.findAll()) {
            String expected = hash(entity.getActor(), entity.getAction(), entity.getEntityType(), entity.getEntityId(), entity.getEventPayload(), entity.getCreatedAt(), previousHash);
            if (!expected.equals(entity.getEventHash())) {
                return false;
            }
            if (previousHash != null && !previousHash.equals(entity.getPreviousHash())) {
                return false;
            }
            previousHash = entity.getEventHash();
        }
        return true;
    }

    @Transactional(readOnly = true)
    public String latestHash() {
        return auditLogRepository.findAll().stream()
                .reduce((left, right) -> right)
                .map(AuditLogEntity::getEventHash)
                .orElse(null);
    }

    private AuditEvent toEvent(AuditLogEntity entity) {
        return new AuditEvent(entity.getAuditId(), entity.getActor(), entity.getAction(), entity.getEntityType(), entity.getEntityId(), entity.getEventPayload(), entity.getCreatedAt());
    }

    public String hash(String actor, String action, String entityType, java.util.UUID entityId, String payload, Instant createdAt, String previousHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(nullToEmpty(previousHash).getBytes());
            digest.update(nullToEmpty(actor).getBytes());
            digest.update(nullToEmpty(action).getBytes());
            digest.update(nullToEmpty(entityType).getBytes());
            digest.update(nullToEmpty(String.valueOf(entityId)).getBytes());
            digest.update(nullToEmpty(payload).getBytes());
            digest.update(nullToEmpty(String.valueOf(createdAt)).getBytes());
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Missing SHA-256 implementation", ex);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

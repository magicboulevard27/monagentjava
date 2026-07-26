package com.monagent.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.monagent.config.RetentionProperties;
import com.monagent.persistence.AuditLogEntity;
import com.monagent.persistence.AuditLogRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditGovernanceServiceTest {

    @Test
    void exportsAndVerifiesAuditChain() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        RetentionProperties retentionProperties = new RetentionProperties(Duration.ofDays(90), Duration.ofDays(90), Duration.ofDays(365), "/tmp/archive");
        AuditGovernanceService service = new AuditGovernanceService(repository, retentionProperties);

        AuditLogEntity first = new AuditLogEntity();
        first.setAuditId(UUID.randomUUID());
        first.setActor("actor-a");
        first.setAction("ACTION_A");
        first.setEntityType("incident");
        first.setEntityId(UUID.randomUUID());
        first.setEventPayload("payload-a");
        first.setCreatedAt(Instant.parse("2026-07-20T10:00:00Z"));
        first.setPreviousHash(null);
        first.setEventHash(service.hash(first.getActor(), first.getAction(), first.getEntityType(), first.getEntityId(), first.getEventPayload(), first.getCreatedAt(), null));

        AuditLogEntity second = new AuditLogEntity();
        second.setAuditId(UUID.randomUUID());
        second.setActor("actor-b");
        second.setAction("ACTION_B");
        second.setEntityType("approval");
        second.setEntityId(UUID.randomUUID());
        second.setEventPayload("payload-b");
        second.setCreatedAt(Instant.parse("2026-07-21T10:00:00Z"));
        second.setPreviousHash(first.getEventHash());
        second.setEventHash(service.hash(second.getActor(), second.getAction(), second.getEntityType(), second.getEntityId(), second.getEventPayload(), second.getCreatedAt(), second.getPreviousHash()));

        when(repository.findAll()).thenReturn(List.of(first, second));
        when(repository.findAllByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(Instant.parse("2026-07-20T00:00:00Z"))).thenReturn(List.of(first, second));

        assertThat(service.exportSince(Instant.parse("2026-07-20T00:00:00Z"))).hasSize(2);
        assertThat(service.verifyIntegrity()).isTrue();
        assertThat(service.retentionCutoff()).isBeforeOrEqualTo(Instant.now());
    }
}

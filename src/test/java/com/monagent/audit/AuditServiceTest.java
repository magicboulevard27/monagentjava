package com.monagent.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.monagent.persistence.AuditLogRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditServiceTest {

    @Test
    void recordsAuditEvents() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditService service = new AuditService(repository);

        AuditEvent event = service.record("actor", "ACTION", "entity", UUID.randomUUID(), "{\"ok\":true}");

        assertThat(event.actor()).isEqualTo("actor");
        assertThat(event.action()).isEqualTo("ACTION");
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }
}

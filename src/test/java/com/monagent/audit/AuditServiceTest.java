package com.monagent.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.monagent.persistence.AuditLogRepository;
import com.monagent.security.DataEncryptionService;
import com.monagent.security.RedactionService;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditServiceTest {

    @Test
    void recordsAuditEvents() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditService service = new AuditService(repository, new RedactionService(), new DataEncryptionService(new com.monagent.security.SecurityCryptoProperties("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")));

        AuditEvent event = service.record("actor", "ACTION", "entity", UUID.randomUUID(), "{\"ok\":true}");

        assertThat(event.actor()).isEqualTo("actor");
        assertThat(event.action()).isEqualTo("ACTION");
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }
}

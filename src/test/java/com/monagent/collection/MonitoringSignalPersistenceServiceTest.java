package com.monagent.collection;

import static org.assertj.core.api.Assertions.assertThat;

import com.monagent.collection.model.HealthSourceSignal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class MonitoringSignalPersistenceServiceTest {

    @Autowired
    private com.monagent.persistence.MonitoringSignalRepository repository;

    @Test
    void canMapSignalToEntityShape() {
        SignalNormalizationService normalizationService = new SignalNormalizationService();
        var normalized = normalizationService.fromHealth(new HealthSourceSignal(UUID.randomUUID(), "svc", "prod", Instant.parse("2026-07-22T10:00:00Z"), "UP", false, "raw-1"));
        var entity = MonitoringSignalMapper.toEntity(normalized);
        assertThat(entity.getSignalName()).isEqualTo("service.health");
        assertThat(entity.getSourceType()).isEqualTo("HEALTH");
    }
}

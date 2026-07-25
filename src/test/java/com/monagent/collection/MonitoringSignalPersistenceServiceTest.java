package com.monagent.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monagent.collection.model.HealthSourceSignal;
import com.monagent.collection.model.NormalizedSignal;
import com.monagent.persistence.MonitoringSignalEntity;
import com.monagent.persistence.MonitoringSignalRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MonitoringSignalPersistenceServiceTest {

    private final MonitoringSignalRepository repository = Mockito.mock(MonitoringSignalRepository.class);
    private final MonitoringSignalPersistenceService service = new MonitoringSignalPersistenceService(repository);
    private final SignalNormalizationService normalizationService = new SignalNormalizationService();

    @Test
    void canMapSignalToEntityShape() {
        var normalized = normalizationService.fromHealth(new HealthSourceSignal(UUID.randomUUID(), "svc", "prod",
                Instant.parse("2026-07-22T10:00:00Z"), "UP", false, "raw-1"));
        var entity = MonitoringSignalMapper.toEntity(normalized);
        assertThat(entity.getSignalName()).isEqualTo("service.health");
        assertThat(entity.getSourceType()).isEqualTo("HEALTH");
    }

    @Test
    void saveIsIdempotentForRepeatedSignals() {
        var normalized = normalizationService.fromHealth(new HealthSourceSignal(UUID.randomUUID(), "svc", "prod",
                Instant.parse("2026-07-22T10:00:00Z"), "UP", false, "raw-1"));
        var entity = MonitoringSignalMapper.toEntity(normalized);
        when(repository.existsById(entity.getSignalId())).thenReturn(false, true);
        when(repository.saveAndFlush(any(MonitoringSignalEntity.class))).thenReturn(entity);
        when(repository.findById(entity.getSignalId())).thenReturn(Optional.of(entity));

        var first = service.save(normalized);
        var second = service.save(normalized);

        assertThat(first.signalId()).isEqualTo(second.signalId());
        verify(repository).saveAndFlush(any(MonitoringSignalEntity.class));
        verify(repository).findById(eq(entity.getSignalId()));
    }

    @Test
    void saveAllSkipsDuplicateRowsInBatchAndInDatabase() {
        var normalized = normalizationService.fromHealth(new HealthSourceSignal(UUID.randomUUID(), "svc", "prod",
                Instant.parse("2026-07-22T10:00:00Z"), "UP", false, "raw-1"));
        var entity = MonitoringSignalMapper.toEntity(normalized);
        when(repository.existsById(entity.getSignalId())).thenReturn(true);
        when(repository.findById(entity.getSignalId())).thenReturn(Optional.of(entity));

        List<NormalizedSignal> results = service.saveAll(List.of(normalized, normalized));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).signalId()).isEqualTo(normalized.signalId());
    }
}

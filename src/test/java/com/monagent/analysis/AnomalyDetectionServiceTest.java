package com.monagent.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monagent.collection.model.NormalizedSignal;
import com.monagent.collection.model.SignalSeverity;
import com.monagent.collection.model.SignalStatus;
import com.monagent.collection.model.SourceType;
import com.monagent.persistence.AnomalyOutcomeRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnomalyDetectionServiceTest {

    @Test
    void detectsCpuThreshold() {
        AnomalyOutcomeRepository repository = mock(AnomalyOutcomeRepository.class);
        AnomalyDetectionService service = new AnomalyDetectionService(repository);
        NormalizedSignal signal = new NormalizedSignal(
                UUID.randomUUID(),
                UUID.randomUUID(),
                SourceType.METRICS,
                "cpu",
                "91.2",
                "%",
                SignalStatus.OK,
                SignalSeverity.NONE,
                Instant.parse("2026-07-22T10:00:00Z"),
                "ref-1");

        var outcome = service.evaluate(signal);

        assertThat(outcome.outcomeStatus()).isEqualTo("TRIGGERED");
        assertThat(outcome.severity()).isEqualTo("HIGH");
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void suppressesNonTriggeredMetric() {
        AnomalyOutcomeRepository repository = mock(AnomalyOutcomeRepository.class);
        AnomalyDetectionService service = new AnomalyDetectionService(repository);
        NormalizedSignal signal = new NormalizedSignal(
                UUID.randomUUID(),
                UUID.randomUUID(),
                SourceType.METRICS,
                "memory",
                "50",
                "%",
                SignalStatus.OK,
                SignalSeverity.NONE,
                Instant.parse("2026-07-22T10:00:00Z"),
                "ref-2");

        var outcome = service.evaluate(signal);

        assertThat(outcome.outcomeStatus()).isEqualTo("SUPPRESSED");
        assertThat(outcome.cooldownUntil()).isNull();
    }
}

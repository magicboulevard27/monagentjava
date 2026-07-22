package com.monagent.collection;

import static org.assertj.core.api.Assertions.assertThat;

import com.monagent.collection.model.CiCdSourceSignal;
import com.monagent.collection.model.HealthSourceSignal;
import com.monagent.collection.model.KubernetesSourceSignal;
import com.monagent.collection.model.LogSourceSignal;
import com.monagent.collection.model.MetricsSourceSignal;
import com.monagent.collection.model.SignalSeverity;
import com.monagent.collection.model.SignalStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SignalNormalizationServiceTest {

    private final SignalNormalizationService service = new SignalNormalizationService();

    @Test
    void normalizesHealthSignals() {
        var signal = service.fromHealth(new HealthSourceSignal(UUID.randomUUID(), "order", "prod", Instant.parse("2026-07-22T10:00:00Z"), "DEGRADED", true, "ref-1"));
        assertThat(signal.sourceType().name()).isEqualTo("HEALTH");
        assertThat(signal.status()).isEqualTo(SignalStatus.DEGRADED);
        assertThat(signal.severity()).isEqualTo(SignalSeverity.HIGH);
        assertThat(signal.signalName()).isEqualTo("service.health");
    }

    @Test
    void normalizesMetricsSignals() {
        var signal = service.fromMetrics(new MetricsSourceSignal(UUID.randomUUID(), "order", "prod", Instant.parse("2026-07-22T10:00:00Z"), "cpu", 91.4, "%", "ref-2"));
        assertThat(signal.status()).isEqualTo(SignalStatus.OK);
        assertThat(signal.severity()).isEqualTo(SignalSeverity.HIGH);
        assertThat(signal.signalName()).isEqualTo("cpu");
    }

    @Test
    void normalizesLogSignalsAndTruncatesLongMessages() {
        var longMessage = "x".repeat(300);
        var signal = service.fromLog(new LogSourceSignal(UUID.randomUUID(), "order", "prod", Instant.parse("2026-07-22T10:00:00Z"), "OutOfMemoryError", longMessage, "ref-3"));
        assertThat(signal.severity()).isEqualTo(SignalSeverity.HIGH);
        assertThat(signal.signalValue()).hasSize(240);
    }

    @Test
    void normalizesKubernetesSignals() {
        var signal = service.fromKubernetes(new KubernetesSourceSignal(UUID.randomUUID(), "order", "prod", Instant.parse("2026-07-22T10:00:00Z"), "Pod", "Warning", "Pod restarted", "ref-4"));
        assertThat(signal.status()).isEqualTo(SignalStatus.DEGRADED);
        assertThat(signal.severity()).isEqualTo(SignalSeverity.MEDIUM);
    }

    @Test
    void normalizesCiCdSignals() {
        var signal = service.fromCiCd(new CiCdSourceSignal(UUID.randomUUID(), "order", "prod", Instant.parse("2026-07-22T10:00:00Z"), "Deployment", "sha-123", "ref-5"));
        assertThat(signal.sourceType().name()).isEqualTo("CICD");
        assertThat(signal.signalName()).isEqualTo("deployment.deployment");
    }
}

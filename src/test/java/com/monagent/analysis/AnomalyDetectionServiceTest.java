package com.monagent.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monagent.api.service.MonitoredServiceService;
import com.monagent.collection.model.NormalizedSignal;
import com.monagent.collection.model.SignalSeverity;
import com.monagent.collection.model.SignalStatus;
import com.monagent.collection.model.SourceType;
import com.monagent.domain.MonitoredService;
import com.monagent.persistence.AnomalyOutcomeRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AnomalyDetectionServiceTest {

    @Test
    void detectsCpuThreshold() {
        AnomalyOutcomeRepository repository = mock(AnomalyOutcomeRepository.class);
        MonitoredServiceService serviceService = mock(MonitoredServiceService.class);
        when(serviceService.get(Mockito.any())).thenReturn(new MonitoredService(
                UUID.randomUUID(),
                "payments",
                "production",
                "team-a",
                "http://example.com/health",
                "payments",
                "payments-*",
                "payments",
                "default",
                "payments",
                List.of("slack"),
                true));
        AnomalyPolicyProperties properties = new AnomalyPolicyProperties(List.of(
                new AnomalyPolicyProperties.Rule(null, null, "cpu", null, new java.math.BigDecimal("80"), ThresholdComparator.GREATER_THAN, "OVER_THRESHOLD", "HIGH", 5, 3, 10, 10)));
        AnomalyDetectionService service = new AnomalyDetectionService(repository, new AnomalyThresholdPolicyService(properties, serviceService));
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
        MonitoredServiceService serviceService = mock(MonitoredServiceService.class);
        when(serviceService.get(Mockito.any())).thenReturn(new MonitoredService(
                UUID.randomUUID(),
                "payments",
                "production",
                "team-a",
                "http://example.com/health",
                "payments",
                "payments-*",
                "payments",
                "default",
                "payments",
                List.of("slack"),
                true));
        AnomalyPolicyProperties properties = new AnomalyPolicyProperties(List.of(
                new AnomalyPolicyProperties.Rule(null, null, "memory", null, new java.math.BigDecimal("85"), ThresholdComparator.GREATER_THAN, "OVER_THRESHOLD", "HIGH", 5, 3, 10, 10)));
        AnomalyDetectionService service = new AnomalyDetectionService(repository, new AnomalyThresholdPolicyService(properties, serviceService));
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

    @Test
    void prefersServiceSpecificPolicyOverGenericPolicy() {
        AnomalyOutcomeRepository repository = mock(AnomalyOutcomeRepository.class);
        MonitoredServiceService serviceService = mock(MonitoredServiceService.class);
        MonitoredService service = new MonitoredService(
                UUID.randomUUID(),
                "orders-api",
                "production",
                "team-a",
                "http://example.com/health",
                "orders",
                "orders-*",
                "orders",
                "default",
                "orders",
                List.of("slack"),
                true);
        when(serviceService.get(service.serviceId())).thenReturn(service);
        AnomalyPolicyProperties properties = new AnomalyPolicyProperties(List.of(
                new AnomalyPolicyProperties.Rule(null, null, "memory", null, new java.math.BigDecimal("85"), ThresholdComparator.GREATER_THAN, "OVER_THRESHOLD", "HIGH", 5, 3, 10, 10),
                new AnomalyPolicyProperties.Rule("orders-api", "production", "memory", null, new java.math.BigDecimal("75"), ThresholdComparator.GREATER_THAN, "OVER_THRESHOLD", "CRITICAL", 5, 3, 10, 10)));
        AnomalyDetectionService detectionService = new AnomalyDetectionService(repository, new AnomalyThresholdPolicyService(properties, serviceService));
        NormalizedSignal signal = new NormalizedSignal(
                UUID.randomUUID(),
                service.serviceId(),
                SourceType.METRICS,
                "memory",
                "80",
                "%",
                SignalStatus.OK,
                SignalSeverity.NONE,
                Instant.parse("2026-07-22T10:00:00Z"),
                "ref-3");

        var outcome = detectionService.evaluate(signal);

        assertThat(outcome.severity()).isEqualTo("CRITICAL");
        assertThat(outcome.thresholdValue()).isEqualByComparingTo("75");
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void detectsContinuouslyIncreasingKafkaLag() {
        AnomalyOutcomeRepository repository = mock(AnomalyOutcomeRepository.class);
        MonitoredServiceService serviceService = mock(MonitoredServiceService.class);
        MonitoredService service = new MonitoredService(
                UUID.randomUUID(),
                "orders",
                "production",
                "team-a",
                "http://example.com/health",
                "orders",
                "orders-*",
                "orders",
                "default",
                "orders",
                List.of("slack"),
                true);
        when(serviceService.get(service.serviceId())).thenReturn(service);
        AnomalyPolicyProperties properties = new AnomalyPolicyProperties(List.of(
                new AnomalyPolicyProperties.Rule(null, null, "kafka.lag", null, new java.math.BigDecimal("100"), ThresholdComparator.INCREASING, "INCREASING", "HIGH", 5, 3, 10, 10)));
        AnomalyThresholdPolicyService policyService = new AnomalyThresholdPolicyService(properties, serviceService);
        AnomalyDetectionService detectionService = new AnomalyDetectionService(repository, policyService);

        when(repository.findTop5ByServiceIdAndMetricNameOrderByDetectedAtDesc(service.serviceId(), "kafka.lag"))
                .thenReturn(List.of(
                        new com.monagent.persistence.AnomalyOutcomeEntity() {{
                            setObservedValue(new java.math.BigDecimal("150"));
                            setDetectedAt(Instant.parse("2026-07-22T10:04:00Z"));
                        }},
                        new com.monagent.persistence.AnomalyOutcomeEntity() {{
                            setObservedValue(new java.math.BigDecimal("130"));
                            setDetectedAt(Instant.parse("2026-07-22T10:03:00Z"));
                        }},
                        new com.monagent.persistence.AnomalyOutcomeEntity() {{
                            setObservedValue(new java.math.BigDecimal("120"));
                            setDetectedAt(Instant.parse("2026-07-22T10:02:00Z"));
                        }}));

        NormalizedSignal signal = new NormalizedSignal(
                UUID.randomUUID(),
                service.serviceId(),
                SourceType.METRICS,
                "kafka.lag",
                "140",
                "messages",
                SignalStatus.OK,
                SignalSeverity.NONE,
                Instant.parse("2026-07-22T10:05:00Z"),
                "ref-kafka-1");

        var outcome = detectionService.evaluate(signal);

        assertThat(outcome.outcomeStatus()).isEqualTo("TRIGGERED");
        assertThat(outcome.severity()).isEqualTo("HIGH");
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void suppressesDuringCooldownWhenWithinHysteresisBand() {
        AnomalyOutcomeRepository repository = mock(AnomalyOutcomeRepository.class);
        MonitoredServiceService serviceService = mock(MonitoredServiceService.class);
        MonitoredService service = new MonitoredService(
                UUID.randomUUID(),
                "payments",
                "production",
                "team-a",
                "http://example.com/health",
                "payments",
                "payments-*",
                "payments",
                "default",
                "payments",
                List.of("slack"),
                true);
        when(serviceService.get(service.serviceId())).thenReturn(service);
        AnomalyPolicyProperties properties = new AnomalyPolicyProperties(List.of(
                new AnomalyPolicyProperties.Rule(null, null, "cpu", null, new java.math.BigDecimal("80"), ThresholdComparator.GREATER_THAN, "OVER_THRESHOLD", "HIGH", 5, 3, 10, 10)));
        AnomalyThresholdPolicyService policyService = new AnomalyThresholdPolicyService(properties, serviceService);
        AnomalyDetectionService detectionService = new AnomalyDetectionService(repository, policyService);

        when(repository.findTopByServiceIdAndMetricNameOrderByDetectedAtDesc(service.serviceId(), "cpu"))
                .thenReturn(java.util.Optional.of(new com.monagent.persistence.AnomalyOutcomeEntity() {{
                    setCooldownUntil(Instant.parse("2026-07-22T10:15:00Z"));
                }}));

        NormalizedSignal signal = new NormalizedSignal(
                UUID.randomUUID(),
                service.serviceId(),
                SourceType.METRICS,
                "cpu",
                "82",
                "%",
                SignalStatus.OK,
                SignalSeverity.NONE,
                Instant.parse("2026-07-22T10:05:00Z"),
                "ref-cooldown");

        var outcome = detectionService.evaluate(signal);

        assertThat(outcome.outcomeStatus()).isEqualTo("SUPPRESSED");
        assertThat(outcome.cooldownUntil()).isNull();
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void suppressesDuplicateSignalWithinWindow() {
        AnomalyOutcomeRepository repository = mock(AnomalyOutcomeRepository.class);
        MonitoredServiceService serviceService = mock(MonitoredServiceService.class);
        MonitoredService service = new MonitoredService(
                UUID.randomUUID(),
                "payments",
                "production",
                "team-a",
                "http://example.com/health",
                "payments",
                "payments-*",
                "payments",
                "default",
                "payments",
                List.of("slack"),
                true);
        when(serviceService.get(service.serviceId())).thenReturn(service);
        AnomalyPolicyProperties properties = new AnomalyPolicyProperties(List.of(
                new AnomalyPolicyProperties.Rule(null, null, "cpu", null, new java.math.BigDecimal("80"), ThresholdComparator.GREATER_THAN, "OVER_THRESHOLD", "HIGH", 5, 3, 10, 10)));
        AnomalyDetectionService detectionService = new AnomalyDetectionService(repository, new AnomalyThresholdPolicyService(properties, serviceService));
        UUID signalId = UUID.randomUUID();

        when(repository.findTopByServiceIdAndMetricNameOrderByDetectedAtDesc(service.serviceId(), "cpu"))
                .thenReturn(java.util.Optional.of(new com.monagent.persistence.AnomalyOutcomeEntity() {{
                    setSignalId(signalId);
                    setObservedValue(new java.math.BigDecimal("91.2"));
                    setDetectedAt(Instant.parse("2026-07-22T10:00:30Z"));
                }}));

        NormalizedSignal signal = new NormalizedSignal(
                signalId,
                service.serviceId(),
                SourceType.METRICS,
                "cpu",
                "91.2",
                "%",
                SignalStatus.OK,
                SignalSeverity.NONE,
                Instant.parse("2026-07-22T10:01:00Z"),
                "ref-dup");

        var outcome = detectionService.evaluate(signal);

        assertThat(outcome.outcomeStatus()).isEqualTo("SUPPRESSED");
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void suppressesFlappingNearThresholdDuringCooldown() {
        AnomalyOutcomeRepository repository = mock(AnomalyOutcomeRepository.class);
        MonitoredServiceService serviceService = mock(MonitoredServiceService.class);
        MonitoredService service = new MonitoredService(
                UUID.randomUUID(),
                "payments",
                "production",
                "team-a",
                "http://example.com/health",
                "payments",
                "payments-*",
                "payments",
                "default",
                "payments",
                List.of("slack"),
                true);
        when(serviceService.get(service.serviceId())).thenReturn(service);
        AnomalyPolicyProperties properties = new AnomalyPolicyProperties(List.of(
                new AnomalyPolicyProperties.Rule(null, null, "cpu", null, new java.math.BigDecimal("80"), ThresholdComparator.GREATER_THAN, "OVER_THRESHOLD", "HIGH", 5, 3, 10, 10)));
        AnomalyDetectionService detectionService = new AnomalyDetectionService(repository, new AnomalyThresholdPolicyService(properties, serviceService));

        when(repository.findTopByServiceIdAndMetricNameOrderByDetectedAtDesc(service.serviceId(), "cpu"))
                .thenReturn(java.util.Optional.of(new com.monagent.persistence.AnomalyOutcomeEntity() {{
                    setCooldownUntil(Instant.parse("2026-07-22T10:15:00Z"));
                    setDetectedAt(Instant.parse("2026-07-22T10:05:00Z"));
                }}));

        NormalizedSignal signal = new NormalizedSignal(
                UUID.randomUUID(),
                service.serviceId(),
                SourceType.METRICS,
                "cpu",
                "82",
                "%",
                SignalStatus.OK,
                SignalSeverity.NONE,
                Instant.parse("2026-07-22T10:06:00Z"),
                "ref-flap");

        var outcome = detectionService.evaluate(signal);

        assertThat(outcome.outcomeStatus()).isEqualTo("SUPPRESSED");
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void suppressesBoundaryValueAtHysteresisLimit() {
        AnomalyOutcomeRepository repository = mock(AnomalyOutcomeRepository.class);
        MonitoredServiceService serviceService = mock(MonitoredServiceService.class);
        MonitoredService service = new MonitoredService(
                UUID.randomUUID(),
                "payments",
                "production",
                "team-a",
                "http://example.com/health",
                "payments",
                "payments-*",
                "payments",
                "default",
                "payments",
                List.of("slack"),
                true);
        when(serviceService.get(service.serviceId())).thenReturn(service);
        AnomalyPolicyProperties properties = new AnomalyPolicyProperties(List.of(
                new AnomalyPolicyProperties.Rule(null, null, "cpu", null, new java.math.BigDecimal("80"), ThresholdComparator.GREATER_THAN, "OVER_THRESHOLD", "HIGH", 5, 3, 10, 10)));
        AnomalyDetectionService detectionService = new AnomalyDetectionService(repository, new AnomalyThresholdPolicyService(properties, serviceService));

        NormalizedSignal signal = new NormalizedSignal(
                UUID.randomUUID(),
                service.serviceId(),
                SourceType.METRICS,
                "cpu",
                "88",
                "%",
                SignalStatus.OK,
                SignalSeverity.NONE,
                Instant.parse("2026-07-22T10:06:00Z"),
                "ref-boundary");

        var outcome = detectionService.evaluate(signal);

        assertThat(outcome.outcomeStatus()).isEqualTo("SUPPRESSED");
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void suppressesNonIncreasingKafkaLagTrend() {
        AnomalyOutcomeRepository repository = mock(AnomalyOutcomeRepository.class);
        MonitoredServiceService serviceService = mock(MonitoredServiceService.class);
        MonitoredService service = new MonitoredService(
                UUID.randomUUID(),
                "orders",
                "production",
                "team-a",
                "http://example.com/health",
                "orders",
                "orders-*",
                "orders",
                "default",
                "orders",
                List.of("slack"),
                true);
        when(serviceService.get(service.serviceId())).thenReturn(service);
        AnomalyPolicyProperties properties = new AnomalyPolicyProperties(List.of(
                new AnomalyPolicyProperties.Rule(null, null, "kafka.lag", null, new java.math.BigDecimal("100"), ThresholdComparator.INCREASING, "INCREASING", "HIGH", 5, 3, 10, 10)));
        AnomalyThresholdPolicyService policyService = new AnomalyThresholdPolicyService(properties, serviceService);
        AnomalyDetectionService detectionService = new AnomalyDetectionService(repository, policyService);

        when(repository.findTop5ByServiceIdAndMetricNameOrderByDetectedAtDesc(service.serviceId(), "kafka.lag"))
                .thenReturn(List.of(
                        new com.monagent.persistence.AnomalyOutcomeEntity() {{
                            setObservedValue(new java.math.BigDecimal("150"));
                            setDetectedAt(Instant.parse("2026-07-22T10:04:00Z"));
                        }},
                        new com.monagent.persistence.AnomalyOutcomeEntity() {{
                            setObservedValue(new java.math.BigDecimal("160"));
                            setDetectedAt(Instant.parse("2026-07-22T10:03:00Z"));
                        }},
                        new com.monagent.persistence.AnomalyOutcomeEntity() {{
                            setObservedValue(new java.math.BigDecimal("120"));
                            setDetectedAt(Instant.parse("2026-07-22T10:02:00Z"));
                        }}));

        NormalizedSignal signal = new NormalizedSignal(
                UUID.randomUUID(),
                service.serviceId(),
                SourceType.METRICS,
                "kafka.lag",
                "170",
                "messages",
                SignalStatus.OK,
                SignalSeverity.NONE,
                Instant.parse("2026-07-22T10:05:00Z"),
                "ref-trend");

        var outcome = detectionService.evaluate(signal);

        assertThat(outcome.outcomeStatus()).isEqualTo("SUPPRESSED");
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void suppressesStaleKafkaLagTrendOutsideEvaluationWindow() {
        AnomalyOutcomeRepository repository = mock(AnomalyOutcomeRepository.class);
        MonitoredServiceService serviceService = mock(MonitoredServiceService.class);
        MonitoredService service = new MonitoredService(
                UUID.randomUUID(),
                "orders",
                "production",
                "team-a",
                "http://example.com/health",
                "orders",
                "orders-*",
                "orders",
                "default",
                "orders",
                List.of("slack"),
                true);
        when(serviceService.get(service.serviceId())).thenReturn(service);
        AnomalyPolicyProperties properties = new AnomalyPolicyProperties(List.of(
                new AnomalyPolicyProperties.Rule(null, null, "kafka.lag", null, new java.math.BigDecimal("100"), ThresholdComparator.INCREASING, "INCREASING", "HIGH", 5, 3, 10, 10)));
        AnomalyDetectionService detectionService = new AnomalyDetectionService(repository, new AnomalyThresholdPolicyService(properties, serviceService));

        when(repository.findTop5ByServiceIdAndMetricNameOrderByDetectedAtDesc(service.serviceId(), "kafka.lag"))
                .thenReturn(List.of(
                        new com.monagent.persistence.AnomalyOutcomeEntity() {{
                            setObservedValue(new java.math.BigDecimal("120"));
                            setDetectedAt(Instant.parse("2026-07-22T09:40:00Z"));
                        }},
                        new com.monagent.persistence.AnomalyOutcomeEntity() {{
                            setObservedValue(new java.math.BigDecimal("130"));
                            setDetectedAt(Instant.parse("2026-07-22T09:41:00Z"));
                        }},
                        new com.monagent.persistence.AnomalyOutcomeEntity() {{
                            setObservedValue(new java.math.BigDecimal("140"));
                            setDetectedAt(Instant.parse("2026-07-22T09:42:00Z"));
                        }}));

        NormalizedSignal signal = new NormalizedSignal(
                UUID.randomUUID(),
                service.serviceId(),
                SourceType.METRICS,
                "kafka.lag",
                "150",
                "messages",
                SignalStatus.OK,
                SignalSeverity.NONE,
                Instant.parse("2026-07-22T10:05:00Z"),
                "ref-stale");

        var outcome = detectionService.evaluate(signal);

        assertThat(outcome.outcomeStatus()).isEqualTo("SUPPRESSED");
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void suppressesFalsePositiveWithinHysteresisBand() {
        AnomalyOutcomeRepository repository = mock(AnomalyOutcomeRepository.class);
        MonitoredServiceService serviceService = mock(MonitoredServiceService.class);
        MonitoredService service = new MonitoredService(
                UUID.randomUUID(),
                "payments",
                "production",
                "team-a",
                "http://example.com/health",
                "payments",
                "payments-*",
                "payments",
                "default",
                "payments",
                List.of("slack"),
                true);
        when(serviceService.get(service.serviceId())).thenReturn(service);
        AnomalyPolicyProperties properties = new AnomalyPolicyProperties(List.of(
                new AnomalyPolicyProperties.Rule(null, null, "memory", null, new java.math.BigDecimal("85"), ThresholdComparator.GREATER_THAN, "OVER_THRESHOLD", "HIGH", 5, 3, 10, 10)));
        AnomalyDetectionService detectionService = new AnomalyDetectionService(repository, new AnomalyThresholdPolicyService(properties, serviceService));

        NormalizedSignal signal = new NormalizedSignal(
                UUID.randomUUID(),
                service.serviceId(),
                SourceType.METRICS,
                "memory",
                "86",
                "%",
                SignalStatus.OK,
                SignalSeverity.NONE,
                Instant.parse("2026-07-22T10:06:00Z"),
                "ref-false-positive");

        var outcome = detectionService.evaluate(signal);

        assertThat(outcome.outcomeStatus()).isEqualTo("SUPPRESSED");
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }
}

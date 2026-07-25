package com.monagent.collection.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.monagent.config.AsyncProcessingProperties;
import com.monagent.collection.scheduling.CollectorRateLimitService;
import com.monagent.persistence.CollectorIdempotencyRepository;
import com.monagent.persistence.CollectorDeadLetterRepository;
import com.monagent.web.SelfObservabilityMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CollectorWorkDispatcherTest {

    @Test
    void tracksBacklogAroundAsyncDispatch() {
        SelfObservabilityMetrics metrics = new SelfObservabilityMetrics(new SimpleMeterRegistry());
        CollectorBacklogMonitor backlogMonitor = new CollectorBacklogMonitor(metrics);
        CollectorDeadLetterRepository deadLetterRepository = Mockito.mock(CollectorDeadLetterRepository.class);
        CollectorIdempotencyRepository idempotencyRepository = Mockito.mock(CollectorIdempotencyRepository.class);
        CollectorRateLimitService rateLimitService = Mockito.mock(CollectorRateLimitService.class);
        Mockito.when(idempotencyRepository.existsByIdempotencyKey(Mockito.any())).thenReturn(false);
        Mockito.when(rateLimitService.tryAcquire(Mockito.any())).thenReturn(true);
        CollectorWorkDispatcher dispatcher = new CollectorWorkDispatcher(
                Executors.newSingleThreadExecutor(),
                backlogMonitor,
                metrics,
                new AsyncProcessingProperties(Duration.ofSeconds(30), 1, 1, 1, 10, Duration.ofSeconds(30), 3, Duration.ofSeconds(1), Duration.ofMinutes(2)),
                deadLetterRepository,
                idempotencyRepository,
                rateLimitService);

        dispatcher.dispatch(new CollectorJob(CollectorJobType.HEALTH, "service-1", "collector:health:service-1")).join();

        assertThat(backlogMonitor.current()).isZero();
        verifyNoMoreInteractions(deadLetterRepository);
    }

    @Test
    void deadLettersRejectedJobsAfterRetryBudget() {
        SelfObservabilityMetrics metrics = new SelfObservabilityMetrics(new SimpleMeterRegistry());
        CollectorBacklogMonitor backlogMonitor = new CollectorBacklogMonitor(metrics);
        CollectorDeadLetterRepository deadLetterRepository = Mockito.mock(CollectorDeadLetterRepository.class);
        CollectorIdempotencyRepository idempotencyRepository = Mockito.mock(CollectorIdempotencyRepository.class);
        CollectorRateLimitService rateLimitService = Mockito.mock(CollectorRateLimitService.class);
        Mockito.when(idempotencyRepository.existsByIdempotencyKey(Mockito.any())).thenReturn(false);
        Mockito.when(rateLimitService.tryAcquire(Mockito.any())).thenReturn(true);
        Executor rejectingExecutor = command -> {
            throw new RejectedExecutionException("queue full");
        };
        CollectorWorkDispatcher dispatcher = new CollectorWorkDispatcher(
                rejectingExecutor,
                backlogMonitor,
                metrics,
                new AsyncProcessingProperties(Duration.ofSeconds(30), 1, 1, 1, 10, Duration.ofSeconds(30), 2, Duration.ofMillis(1), Duration.ofMinutes(2)),
                deadLetterRepository,
                idempotencyRepository,
                rateLimitService);

        CollectorWorkResult result = dispatcher.dispatch(new CollectorJob(CollectorJobType.HEALTH, "service-1", "collector:health:service-1")).join();

        assertThat(result.accepted()).isFalse();
        assertThat(result.status()).isEqualTo("dead-lettered");
        assertThat(backlogMonitor.current()).isZero();
        verify(deadLetterRepository).saveAndFlush(Mockito.any());
    }

    @Test
    void skipsDuplicateJobByIdempotencyKey() {
        SelfObservabilityMetrics metrics = new SelfObservabilityMetrics(new SimpleMeterRegistry());
        CollectorBacklogMonitor backlogMonitor = new CollectorBacklogMonitor(metrics);
        CollectorDeadLetterRepository deadLetterRepository = Mockito.mock(CollectorDeadLetterRepository.class);
        CollectorIdempotencyRepository idempotencyRepository = Mockito.mock(CollectorIdempotencyRepository.class);
        CollectorRateLimitService rateLimitService = Mockito.mock(CollectorRateLimitService.class);
        Mockito.when(idempotencyRepository.existsByIdempotencyKey("collector:health:service-1")).thenReturn(true);
        Mockito.when(rateLimitService.tryAcquire(Mockito.any())).thenReturn(true);
        CollectorWorkDispatcher dispatcher = new CollectorWorkDispatcher(
                Executors.newSingleThreadExecutor(),
                backlogMonitor,
                metrics,
                new AsyncProcessingProperties(Duration.ofSeconds(30), 1, 1, 1, 10, Duration.ofSeconds(30), 2, Duration.ofMillis(1), Duration.ofMinutes(2)),
                deadLetterRepository,
                idempotencyRepository,
                rateLimitService);

        CollectorWorkResult result = dispatcher.dispatch(new CollectorJob(CollectorJobType.HEALTH, "service-1", "collector:health:service-1")).join();

        assertThat(result.accepted()).isTrue();
        assertThat(result.status()).isEqualTo("duplicate");
        assertThat(backlogMonitor.current()).isZero();
        verifyNoMoreInteractions(deadLetterRepository);
    }

    @Test
    void appliesBackpressureWhenQueueIsFull() {
        SelfObservabilityMetrics metrics = new SelfObservabilityMetrics(new SimpleMeterRegistry());
        CollectorBacklogMonitor backlogMonitor = new CollectorBacklogMonitor(metrics);
        CollectorDeadLetterRepository deadLetterRepository = Mockito.mock(CollectorDeadLetterRepository.class);
        CollectorIdempotencyRepository idempotencyRepository = Mockito.mock(CollectorIdempotencyRepository.class);
        CollectorRateLimitService rateLimitService = Mockito.mock(CollectorRateLimitService.class);
        Mockito.when(idempotencyRepository.existsByIdempotencyKey(Mockito.any())).thenReturn(false);
        Mockito.when(rateLimitService.tryAcquire(Mockito.any())).thenReturn(false);
        CollectorWorkDispatcher dispatcher = new CollectorWorkDispatcher(
                Executors.newSingleThreadExecutor(),
                backlogMonitor,
                metrics,
                new AsyncProcessingProperties(Duration.ofSeconds(30), 1, 1, 1, 0, Duration.ofSeconds(30), 2, Duration.ofMillis(1), Duration.ofMinutes(2)),
                deadLetterRepository,
                idempotencyRepository,
                rateLimitService);

        CollectorWorkResult result = dispatcher.dispatch(new CollectorJob(CollectorJobType.HEALTH, "service-1", "collector:health:service-1")).join();

        assertThat(result.accepted()).isFalse();
        assertThat(result.status()).isEqualTo("rate-limited");
        assertThat(backlogMonitor.current()).isZero();
        verifyNoMoreInteractions(deadLetterRepository);
    }
}

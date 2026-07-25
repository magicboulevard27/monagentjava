package com.monagent.collection.scheduling;

import com.monagent.config.AsyncProcessingProperties;
import com.monagent.persistence.CollectorIdempotencyEntity;
import com.monagent.persistence.CollectorIdempotencyRepository;
import com.monagent.persistence.CollectorDeadLetterEntity;
import com.monagent.persistence.CollectorDeadLetterRepository;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import com.monagent.web.SelfObservabilityMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CollectorWorkDispatcher {

    private static final Logger log = LoggerFactory.getLogger(CollectorWorkDispatcher.class);

    private final Executor executor;
    private final CollectorBacklogMonitor backlogMonitor;
    private final SelfObservabilityMetrics metrics;
    private final AsyncProcessingProperties properties;
    private final CollectorDeadLetterRepository deadLetterRepository;
    private final CollectorIdempotencyRepository idempotencyRepository;
    private final CollectorRateLimitService rateLimitService;

    public CollectorWorkDispatcher(
            @Qualifier("collectorWorkerExecutor") Executor executor,
            CollectorBacklogMonitor backlogMonitor,
            SelfObservabilityMetrics metrics,
            AsyncProcessingProperties properties,
            CollectorDeadLetterRepository deadLetterRepository,
            CollectorIdempotencyRepository idempotencyRepository,
            CollectorRateLimitService rateLimitService) {
        this.executor = executor;
        this.backlogMonitor = backlogMonitor;
        this.metrics = metrics;
        this.properties = properties;
        this.deadLetterRepository = deadLetterRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.rateLimitService = rateLimitService;
    }

    public CompletableFuture<CollectorWorkResult> dispatch(CollectorJob job) {
        validate(job);
        if (!registerIdempotency(job)) {
            metrics.incrementCollectorSuccess(job.type().name());
            return CompletableFuture.completedFuture(new CollectorWorkResult(job, true, "duplicate"));
        }
        if (!rateLimitService.tryAcquire(rateLimitKey(job))) {
            metrics.incrementCollectorFailure(job.type().name());
            return CompletableFuture.completedFuture(new CollectorWorkResult(job, false, "rate-limited"));
        }
        backlogMonitor.increment();
        try {
            return submitWithRetry(job, 1).whenComplete((result, error) -> rateLimitService.release());
        } catch (RuntimeException ex) {
            backlogMonitor.decrement();
            rateLimitService.release();
            recordDeadLetter(job, 1, ex);
            metrics.incrementCollectorFailure(job.type().name());
            return CompletableFuture.completedFuture(new CollectorWorkResult(job, false, "dead-lettered"));
        }
    }

    private CompletableFuture<CollectorWorkResult> submitWithRetry(CollectorJob job, int attempt) {
        try {
            return CompletableFuture.supplyAsync(() -> new CollectorWorkResult(job, true, "queued"), executor)
                    .whenComplete((result, error) -> {
                        backlogMonitor.decrement();
                        if (error == null) {
                            metrics.incrementCollectorSuccess(job.type().name());
                        } else {
                            recordDeadLetter(job, attempt, error);
                            metrics.incrementCollectorFailure(job.type().name());
                        }
                    });
        } catch (RejectedExecutionException ex) {
            if (attempt < properties.dispatchRetryAttempts()) {
                sleepBackoff();
                return submitWithRetry(job, attempt + 1);
            }
            recordDeadLetter(job, attempt, ex);
            metrics.incrementCollectorFailure(job.type().name());
            backlogMonitor.decrement();
            rateLimitService.release();
            return CompletableFuture.completedFuture(new CollectorWorkResult(job, false, "dead-lettered"));
        }
    }

    private void recordDeadLetter(CollectorJob job, int attempt, Throwable error) {
        CollectorDeadLetterEntity entity = new CollectorDeadLetterEntity();
        entity.setDeadLetterId(UUID.randomUUID());
        entity.setJobType(job.type().name());
        entity.setServiceId(job.serviceId());
        entity.setAttemptCount(attempt);
        entity.setReason(error == null ? "unknown failure" : error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
        entity.setPayload(job.type().name() + ":" + job.serviceId());
        entity.setFailedAt(Instant.now());
        deadLetterRepository.saveAndFlush(entity);
        log.warn("Moved collector job to dead letter jobType={} serviceId={} attempts={}", job.type(), job.serviceId(), attempt);
    }

    private void validate(CollectorJob job) {
        if (job == null || job.type() == null || job.serviceId() == null || job.serviceId().isBlank()) {
            throw new IllegalArgumentException("Collector job must include a type and serviceId");
        }
    }

    private void sleepBackoff() {
        try {
            Thread.sleep(Math.max(1L, properties.dispatchRetryBackoff().toMillis()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean registerIdempotency(CollectorJob job) {
        String key = job.idempotencyKey();
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Collector job must include an idempotency key");
        }
        if (idempotencyRepository.existsByIdempotencyKey(key)) {
            return false;
        }
        CollectorIdempotencyEntity entity = new CollectorIdempotencyEntity();
        entity.setKeyId(UUID.randomUUID());
        entity.setIdempotencyKey(key);
        entity.setJobType(job.type().name());
        entity.setServiceId(job.serviceId());
        entity.setRecordedAt(Instant.now());
        idempotencyRepository.saveAndFlush(entity);
        return true;
    }

    private String rateLimitKey(CollectorJob job) {
        return job.type().name() + ":" + job.serviceId();
    }
}

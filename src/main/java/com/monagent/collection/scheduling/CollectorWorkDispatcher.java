package com.monagent.collection.scheduling;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import com.monagent.web.SelfObservabilityMetrics;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CollectorWorkDispatcher {

    private final Executor executor;
    private final CollectorBacklogMonitor backlogMonitor;
    private final SelfObservabilityMetrics metrics;

    public CollectorWorkDispatcher(
            @Qualifier("collectorWorkerExecutor") Executor executor,
            CollectorBacklogMonitor backlogMonitor,
            SelfObservabilityMetrics metrics) {
        this.executor = executor;
        this.backlogMonitor = backlogMonitor;
        this.metrics = metrics;
    }

    public CompletableFuture<CollectorWorkResult> dispatch(CollectorJob job) {
        backlogMonitor.increment();
        return CompletableFuture.supplyAsync(() -> new CollectorWorkResult(job, true, "queued"), executor)
                .whenComplete((result, error) -> {
                    backlogMonitor.decrement();
                    if (error == null) {
                        metrics.incrementCollectorSuccess(job.type().name());
                    } else {
                        metrics.incrementCollectorFailure(job.type().name());
                    }
                });
    }
}

package com.monagent.collection.scheduling;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CollectorWorkDispatcher {

    private final Executor executor;
    private final CollectorBacklogMonitor backlogMonitor;

    public CollectorWorkDispatcher(
            @Qualifier("collectorWorkerExecutor") Executor executor,
            CollectorBacklogMonitor backlogMonitor) {
        this.executor = executor;
        this.backlogMonitor = backlogMonitor;
    }

    public CompletableFuture<CollectorWorkResult> dispatch(CollectorJob job) {
        backlogMonitor.increment();
        return CompletableFuture.supplyAsync(() -> new CollectorWorkResult(job, true, "queued"), executor)
                .whenComplete((result, error) -> backlogMonitor.decrement());
    }
}

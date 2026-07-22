package com.monagent.collection.scheduling;

import java.util.concurrent.atomic.AtomicInteger;
import com.monagent.web.SelfObservabilityMetrics;
import org.springframework.stereotype.Component;

@Component
public class CollectorBacklogMonitor {

    private final AtomicInteger queued = new AtomicInteger();
    private final SelfObservabilityMetrics metrics;

    public CollectorBacklogMonitor(SelfObservabilityMetrics metrics) {
        this.metrics = metrics;
    }

    public int increment() {
        int value = queued.incrementAndGet();
        metrics.setBacklogGauge(value);
        return value;
    }

    public int decrement() {
        int value = queued.updateAndGet(current -> Math.max(0, current - 1));
        metrics.setBacklogGauge(value);
        return value;
    }

    public int current() {
        return queued.get();
    }
}

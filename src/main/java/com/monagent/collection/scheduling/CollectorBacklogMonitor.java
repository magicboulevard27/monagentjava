package com.monagent.collection.scheduling;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class CollectorBacklogMonitor {

    private final AtomicInteger queued = new AtomicInteger();

    public int increment() {
        return queued.incrementAndGet();
    }

    public int decrement() {
        return queued.updateAndGet(value -> Math.max(0, value - 1));
    }

    public int current() {
        return queued.get();
    }
}

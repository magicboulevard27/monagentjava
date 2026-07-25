package com.monagent.collection.scheduling;

import com.monagent.config.AsyncProcessingProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class CollectorRateLimitService {

    private final AsyncProcessingProperties properties;
    private final AtomicInteger inFlight = new AtomicInteger();
    private final Map<String, Instant> lastDispatchBySource = new ConcurrentHashMap<>();

    public CollectorRateLimitService(AsyncProcessingProperties properties) {
        this.properties = properties;
    }

    public boolean allowBackpressure() {
        return properties.maxQueuedJobs() <= 0 || inFlight.get() < properties.maxQueuedJobs();
    }

    public boolean allowSource(String sourceKey) {
        Instant now = Instant.now();
        Duration cooldown = properties.perSourceCooldown();
        Instant last = lastDispatchBySource.get(sourceKey);
        if (last != null && cooldown != null && !cooldown.isZero() && last.plus(cooldown).isAfter(now)) {
            return false;
        }
        lastDispatchBySource.put(sourceKey, now);
        return true;
    }

    public boolean tryAcquire(String sourceKey) {
        if (!allowBackpressure()) {
            return false;
        }
        if (!allowSource(sourceKey)) {
            return false;
        }
        inFlight.incrementAndGet();
        return true;
    }

    public void release() {
        inFlight.updateAndGet(current -> Math.max(0, current - 1));
    }
}

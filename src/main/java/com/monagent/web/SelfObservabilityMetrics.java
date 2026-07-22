package com.monagent.web;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Callable;
import org.springframework.stereotype.Component;

@Component
public class SelfObservabilityMetrics {

    private final MeterRegistry meterRegistry;
    private final AtomicInteger backlogGauge = new AtomicInteger();

    public SelfObservabilityMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        meterRegistry.gauge("monagent.collector.backlog", backlogGauge);
    }

    public void incrementCollectorSuccess(String collector) {
        Counter.builder("monagent.collector.success")
                .tag("collector", collector)
                .register(meterRegistry)
                .increment();
    }

    public void incrementCollectorFailure(String collector) {
        Counter.builder("monagent.collector.failure")
                .tag("collector", collector)
                .register(meterRegistry)
                .increment();
    }

    public <T> T time(String metricName, String stage, Callable<T> callable) throws Exception {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return callable.call();
        } finally {
            sample.stop(Timer.builder(metricName).tag("stage", stage).register(meterRegistry));
        }
    }

    public void incrementNotificationDelivery(String channel, boolean delivered) {
        Counter.builder("monagent.notification.delivery")
                .tag("channel", channel)
                .tag("status", delivered ? "delivered" : "failed")
                .register(meterRegistry)
                .increment();
    }

    public void incrementApprovalDecision(String status) {
        Counter.builder("monagent.approval.decision")
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    public void setBacklogGauge(int queued) {
        backlogGauge.set(queued);
    }
}

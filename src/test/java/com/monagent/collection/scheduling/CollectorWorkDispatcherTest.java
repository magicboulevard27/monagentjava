package com.monagent.collection.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import com.monagent.web.SelfObservabilityMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class CollectorWorkDispatcherTest {

    @Test
    void tracksBacklogAroundAsyncDispatch() {
        SelfObservabilityMetrics metrics = new SelfObservabilityMetrics(new SimpleMeterRegistry());
        CollectorBacklogMonitor backlogMonitor = new CollectorBacklogMonitor(metrics);
        CollectorWorkDispatcher dispatcher = new CollectorWorkDispatcher(Executors.newSingleThreadExecutor(), backlogMonitor, metrics);

        dispatcher.dispatch(new CollectorJob(CollectorJobType.HEALTH, "service-1")).join();

        assertThat(backlogMonitor.current()).isZero();
    }
}

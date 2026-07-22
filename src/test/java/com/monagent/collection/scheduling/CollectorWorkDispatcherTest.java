package com.monagent.collection.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class CollectorWorkDispatcherTest {

    @Test
    void tracksBacklogAroundAsyncDispatch() {
        CollectorBacklogMonitor backlogMonitor = new CollectorBacklogMonitor();
        CollectorWorkDispatcher dispatcher = new CollectorWorkDispatcher(Executors.newSingleThreadExecutor(), backlogMonitor);

        dispatcher.dispatch(new CollectorJob(CollectorJobType.HEALTH, "service-1")).join();

        assertThat(backlogMonitor.current()).isZero();
    }
}

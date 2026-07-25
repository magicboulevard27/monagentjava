package com.monagent.collection.scheduling;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monagent.api.service.MonitoredServiceService;
import com.monagent.config.AsyncProcessingProperties;
import com.monagent.domain.MonitoredService;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ScheduledCollectorCoordinatorTest {

    @Test
    void skipsDispatchWhenNotLeader() {
        MonitoredServiceService monitoredServiceService = Mockito.mock(MonitoredServiceService.class);
        CollectorWorkDispatcher dispatcher = Mockito.mock(CollectorWorkDispatcher.class);
        CollectorLeaderElectionService leaderElectionService = Mockito.mock(CollectorLeaderElectionService.class);
        AsyncProcessingProperties properties = new AsyncProcessingProperties(
                Duration.ofSeconds(30),
                2,
                10,
                4,
                100,
                Duration.ofSeconds(30),
                3,
                Duration.ofSeconds(1),
                Duration.ofMinutes(2));
        ScheduledCollectorCoordinator coordinator = new ScheduledCollectorCoordinator(
                monitoredServiceService,
                dispatcher,
                leaderElectionService,
                properties);

        when(leaderElectionService.tryAcquire(properties.leaderLeaseDuration())).thenReturn(false);

        coordinator.scheduleCollectors();

        verify(monitoredServiceService, never()).listEnabled();
        verify(dispatcher, never()).dispatch(Mockito.any());
    }

    @Test
    void dispatchesWhenLeader() {
        MonitoredServiceService monitoredServiceService = Mockito.mock(MonitoredServiceService.class);
        CollectorWorkDispatcher dispatcher = Mockito.mock(CollectorWorkDispatcher.class);
        CollectorLeaderElectionService leaderElectionService = Mockito.mock(CollectorLeaderElectionService.class);
        AsyncProcessingProperties properties = new AsyncProcessingProperties(
                Duration.ofSeconds(30),
                2,
                10,
                4,
                100,
                Duration.ofSeconds(30),
                3,
                Duration.ofSeconds(1),
                Duration.ofMinutes(2));
        ScheduledCollectorCoordinator coordinator = new ScheduledCollectorCoordinator(
                monitoredServiceService,
                dispatcher,
                leaderElectionService,
                properties);
        MonitoredService service = new MonitoredService(
                UUID.randomUUID(),
                "orders",
                "prod",
                "team-a",
                "http://orders/actuator/health",
                "orders",
                "orders-*",
                "orders",
                "default",
                "orders",
                List.of("slack"),
                true);

        when(leaderElectionService.tryAcquire(properties.leaderLeaseDuration())).thenReturn(true);
        when(monitoredServiceService.listEnabled()).thenReturn(List.of(service));

        coordinator.scheduleCollectors();

        verify(monitoredServiceService, times(1)).listEnabled();
        verify(dispatcher, times(5)).dispatch(Mockito.any());
    }
}

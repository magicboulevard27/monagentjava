package com.monagent.api.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.monagent.persistence.MonitoredServiceRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MonitoredServiceServiceTest {

    private final MonitoredServiceRepository repository = mock(MonitoredServiceRepository.class);
    private final MonitoredServiceService service = new MonitoredServiceService(repository);

    @Test
    void rejectsUnsafeHealthUrls() {
        assertThatThrownBy(() -> service.create(new MonitoredServiceRequest(
                "orders",
                "staging",
                "team",
                "file:///etc/passwd",
                null,
                null,
                null,
                null,
                null,
                java.util.List.of(),
                true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported health URL scheme");

        assertThatThrownBy(() -> service.create(new MonitoredServiceRequest(
                "orders",
                "staging",
                "team",
                "https://user:pass@orders.internal/actuator/health",
                null,
                null,
                null,
                null,
                null,
                java.util.List.of(),
                true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("credentials");
    }

    @Test
    void acceptsNormalHttpsHealthUrls() {
        var request = new MonitoredServiceRequest(
                "orders",
                "staging",
                "team",
                "https://orders.internal/actuator/health",
                null,
                null,
                null,
                null,
                null,
                java.util.List.of(),
                true);
        when(repository.saveAndFlush(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create(request);

        org.assertj.core.api.Assertions.assertThat(created.serviceId()).isNotNull();
    }
}

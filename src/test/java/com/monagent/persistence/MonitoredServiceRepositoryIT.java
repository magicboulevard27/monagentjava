package com.monagent.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
class MonitoredServiceRepositoryIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("monagent")
            .withUsername("monagent")
            .withPassword("monagent");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private MonitoredServiceRepository repository;

    @Test
    void savesAndReadsMonitoredService() {
        MonitoredServiceEntity entity = new MonitoredServiceEntity(UUID.randomUUID());
        entity.setServiceName("order-service");
        entity.setEnvironment("staging");
        entity.setOwnerTeam("payments");
        entity.setHealthUrl("https://order-service.internal/actuator/health");
        entity.setAlertChannels("slack,email");
        entity.setEnabled(true);

        repository.saveAndFlush(entity);

        List<MonitoredServiceEntity> services = repository.findAll();
        assertThat(services).hasSize(1);
        assertThat(services.getFirst().getServiceName()).isEqualTo("order-service");
    }
}

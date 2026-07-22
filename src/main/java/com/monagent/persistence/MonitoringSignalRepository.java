package com.monagent.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitoringSignalRepository extends JpaRepository<MonitoringSignalEntity, UUID> {
}

package com.monagent.persistence;

import java.util.UUID;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnomalyOutcomeRepository extends JpaRepository<AnomalyOutcomeEntity, UUID> {
    Optional<AnomalyOutcomeEntity> findTopByServiceIdAndMetricNameOrderByDetectedAtDesc(UUID serviceId, String metricName);
    List<AnomalyOutcomeEntity> findTop5ByServiceIdAndMetricNameOrderByDetectedAtDesc(UUID serviceId, String metricName);
}

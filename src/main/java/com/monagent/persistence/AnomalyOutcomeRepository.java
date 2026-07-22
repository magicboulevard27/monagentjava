package com.monagent.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnomalyOutcomeRepository extends JpaRepository<AnomalyOutcomeEntity, UUID> {
}

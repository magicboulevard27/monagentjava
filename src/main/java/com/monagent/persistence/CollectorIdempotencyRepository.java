package com.monagent.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectorIdempotencyRepository extends JpaRepository<CollectorIdempotencyEntity, UUID> {
    boolean existsByIdempotencyKey(String idempotencyKey);
}

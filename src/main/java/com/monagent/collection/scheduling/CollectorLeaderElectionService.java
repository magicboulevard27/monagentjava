package com.monagent.collection.scheduling;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CollectorLeaderElectionService {

    private static final String LOCK_NAME = "collector-scheduler";

    private final JdbcTemplate jdbcTemplate;
    private final String ownerId = UUID.randomUUID().toString();

    public CollectorLeaderElectionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean tryAcquire(Duration leaseDuration) {
        Instant now = Instant.now();
        Instant leaseExpiresAt = now.plus(leaseDuration);
        int updated = jdbcTemplate.update("""
                UPDATE scheduler_leader_locks
                   SET owner_id = ?,
                       lease_expires_at = ?,
                       updated_at = ?
                 WHERE lock_name = ?
                   AND lease_expires_at < ?
                """, ownerId, leaseExpiresAt, now, LOCK_NAME, now);
        if (updated > 0) {
            return true;
        }

        try {
            int inserted = jdbcTemplate.update("""
                    INSERT INTO scheduler_leader_locks (lock_name, owner_id, lease_expires_at, updated_at)
                    VALUES (?, ?, ?, ?)
                    """, LOCK_NAME, ownerId, leaseExpiresAt, now);
            if (inserted > 0) {
                return true;
            }
        } catch (DuplicateKeyException ignored) {
        }

        Integer current = jdbcTemplate.queryForObject("""
                SELECT CASE WHEN owner_id = ? AND lease_expires_at >= ? THEN 1 ELSE 0 END
                  FROM scheduler_leader_locks
                 WHERE lock_name = ?
                """, Integer.class, ownerId, now, LOCK_NAME);
        return current != null && current == 1;
    }
}

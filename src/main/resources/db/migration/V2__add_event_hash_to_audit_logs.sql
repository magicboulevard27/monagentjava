ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS event_hash VARCHAR(128);

UPDATE audit_logs
SET event_hash = COALESCE(event_hash, '')
WHERE event_hash IS NULL;

ALTER TABLE audit_logs
    ALTER COLUMN event_hash SET NOT NULL;

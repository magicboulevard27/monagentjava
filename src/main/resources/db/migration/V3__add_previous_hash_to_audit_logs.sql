ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS previous_hash VARCHAR(128);

# Incident, Rollback, Backup, Restore, and Disaster Recovery

## Incident Handling

1. Capture the incident window and affected services.
2. Save correlation IDs, incident IDs, and relevant log lines.
3. Preserve evidence before making changes.

## Rollback

- Prefer rollback when a release breaks startup, health, persistence, or approval flow.
- Roll back to the last known-good tagged image or chart release.
- Re-verify health, incident creation, and notification delivery after rollback.

## Backup and Restore

- Back up the PostgreSQL database before major upgrades.
- Restore from the most recent known-good backup if corruption or data loss is suspected.
- Re-run migrations only after the data set is stable.

## Disaster Recovery

- Rebuild the application in a clean environment.
- Restore the database and validate the application starts.
- Verify the collectors, analysis, and notification paths before reopening the system.

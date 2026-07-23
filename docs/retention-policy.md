# Retention Policy

This project defines the following default retention windows:

- Monitoring signals: `P90D`
- Incident evidence: `P365D`
- Audit logs: `P365D`

Archive location:

- Default archive location: `/var/lib/monagent/archive`

Notes:

- Retention values are configurable through `MONAGENT_RETENTION_*` environment variables.
- The policy is defined for the application and deployment documents even when physical purge jobs are not yet enabled.

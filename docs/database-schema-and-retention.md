# Database Schema and Retention

## Schema

The application stores its operational data in PostgreSQL through Flyway-managed migrations under [`src/main/resources/db/migration`](/Z:/_Dev/AI-agents/monagentjava/src/main/resources/db/migration).

Current core tables:

- `monitored_services`
- `monitoring_signals`
- `incidents`
- `incident_evidence`
- `recommendations`
- `approvals`
- `audit_logs`

## Current Migration Set

- `V1__create_monitoring_schema.sql`
- `V2__add_event_hash_to_audit_logs.sql`
- `V3__add_previous_hash_to_audit_logs.sql`

## Retention Defaults

- Monitoring signals: `P90D`
- Incident evidence: `P365D`
- Audit logs: `P365D`
- Archive location: `/var/lib/monagent/archive`

## Operational Notes

- Keep schema changes in Flyway migrations only
- Validate migrations before release
- Use the retention policy document for purge and archive expectations
- Treat audit data as append-only operational history

# Observability Dashboards and Alerts

## Scope

This document defines the minimum internal observability surfaces for the monitoring agent:

- API
- collectors
- workers
- database
- external integrations

## Dashboard Coverage

Create dashboards for:

1. API latency, request rate, error rate, and health endpoint availability.
2. Collector success/failure rates, queue backlog, and source-query latency.
3. Worker throughput, backlog, retries, and dead letters.
4. Database pool usage, migration status, and connection failures.
5. External integration latency and failure rates for logs, traces, Prometheus, Ollama, and notification channels.

## Alert Coverage

Create alerts for:

1. `DOWN` or `DEGRADED` readiness states.
2. Collector failure spikes or sustained backlog growth.
3. Source-query latency above the approved threshold.
4. AI-reasoning latency above the approved threshold.
5. Notification delivery failures and repeated retry exhaustion.
6. Database connection failures or pool exhaustion.
7. Error-rate spikes in the API or worker paths.

## Data Hygiene

- Do not display raw secrets, tokens, passwords, or API keys in dashboard labels or alert bodies.
- Prefer correlation IDs, incident IDs, service names, and metric names.
- Use redacted payloads when linking from alerts to evidence.

## Recommended Sources

- Prometheus for application metrics
- Grafana for dashboards
- Alertmanager or the platform alerting system for notification routing
- OpenTelemetry traces for request and analysis timing


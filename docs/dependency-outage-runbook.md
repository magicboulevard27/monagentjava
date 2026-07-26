# Dependency Outage Runbook

## Scope

Use this runbook when the monitoring agent is operating with one or more unavailable dependencies:

- PostgreSQL
- log search backend
- tracing backend
- Prometheus
- Ollama

The agent is designed to degrade gracefully when optional dependencies fail. PostgreSQL is the mandatory dependency that must be restored before the system is considered fully healthy.

## Recovery Priorities

1. Restore PostgreSQL connectivity first.
2. Restore the highest-value optional dependency for the current incident.
3. Re-enable full signal collection after the dependency has recovered.
4. Capture evidence, timings, and the exact failure window before closing the incident.

## Database Outage

Symptoms:

- `/actuator/health` reports `DOWN`
- API requests fail or return persistence errors
- collector work cannot store signals, incidents, approvals, or audit records

Actions:

1. Check PostgreSQL pod, service, or managed database availability.
2. Verify database credentials, network policies, DNS, and TLS settings.
3. Check migration status and disk availability.
4. Restore the database from the latest known-good backup if corruption or data loss is suspected.
5. Restart the application only after the database is reachable and valid.

Verification:

- `/actuator/health` returns `UP`
- application logs show successful database connectivity
- collectors resume normal persistence

## Log Search Outage

Symptoms:

- log analysis reports degraded or unavailable query results
- log-derived evidence bundles are missing or empty

Actions:

1. Check the log backend endpoint and authentication configuration.
2. Verify the backend index or cluster is healthy.
3. Confirm the configured base URL uses the correct scheme and credentials.
4. Reduce query scope or window size if the backend is overloaded.
5. Restore the backend or switch to the last known-good endpoint if applicable.

Verification:

- log analysis continues with empty fallback responses instead of crashing
- alerts and incidents still include health, metric, and trace evidence

## Trace Backend Outage

Symptoms:

- trace evidence is missing
- dependency-path analysis is incomplete
- trace query latency increases or timeouts occur

Actions:

1. Check Jaeger or Tempo endpoint reachability.
2. Confirm the trace backend is ingesting data and the query window is valid.
3. Verify service name, operation, and status filters are correct.
4. If the backend is temporarily unavailable, allow the agent to continue using other sources.
5. Restore the trace backend before investigating dependency chains in depth.

Verification:

- trace analysis returns fallback results rather than failing the run
- incidents still progress using metrics, logs, and health data

## Prometheus Outage

Symptoms:

- metric collection fails for one or more queries
- anomaly detection has partial or stale input

Actions:

1. Check Prometheus availability and scrape status.
2. Validate the query endpoint, authentication, and TLS settings.
3. Reduce query load if the server is overloaded.
4. Restore the backend or point the agent at the correct Prometheus instance.

Verification:

- failed Prometheus queries are skipped without failing the batch
- other metric series continue to collect when available

## Ollama Outage

Symptoms:

- AI analysis falls back to rule-based guidance
- incident summaries lack LLM-generated refinement

Actions:

1. Check the Ollama service endpoint and model availability.
2. Verify the model name and memory/CPU headroom.
3. Restart the model runtime if it is hung or overloaded.
4. Confirm the circuit breaker closes after the service stabilizes.

Verification:

- the agent emits deterministic fallback analysis
- rule-based alerts and evidence bundles still appear

## General Recovery Checklist

1. Confirm the incident scope and the affected dependency.
2. Capture timestamps, request IDs, and relevant log excerpts.
3. Restore the dependency or switch to the approved fallback path.
4. Verify `/actuator/health` and the affected workflow path.
5. Re-run a representative collection or analysis cycle.
6. Record the incident outcome and any configuration changes.

## Escalation Criteria

Escalate to the platform or database owner when:

- PostgreSQL is unavailable for more than the approved recovery window.
- data loss or corruption is suspected.
- repeated outages occur after recovery.
- a dependency requires manual intervention outside the normal operator permissions.


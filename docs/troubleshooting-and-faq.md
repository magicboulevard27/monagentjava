# Troubleshooting and FAQ

## Common Problems

- App will not start: check the database, config, and migration status first.
- Health endpoint is down: confirm the app has enough time to finish startup and that the database is reachable.
- Compose network errors: remove stale networks and restart the stack.
- Helm upgrade issues: compare the rendered chart with the previous release before rolling forward.
- Missing alerts: verify thresholds, notification channels, and downstream credentials.

## Frequently Asked Questions

- Why is the local LLM optional? Because the system can still produce deterministic fallback analysis without it.
- Why does the collector have extra Kubernetes permissions? It needs read-only namespace access to gather deployment context.
- Why are secrets not documented inline? They should be injected through environment variables or mounted secret files.

## Escalation

- Escalate persistent database failures to the database owner.
- Escalate repeated dependency outages to the platform owner.
- Escalate approval-policy questions to the service owner or security owner.

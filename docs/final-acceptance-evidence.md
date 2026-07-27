# Final Acceptance Evidence

## Supported by Code and Tests

- Registered microservices can be discovered and monitored
  - Service registry, collectors, analysis, and notification paths are implemented.
- Health, metrics, logs, traces, and deployment context can be collected
  - Health and Prometheus collectors, log and trace adapters, and Kubernetes/deployment context collectors are present.
- Sensitive data is redacted before AI processing and notification
  - Redaction services and redaction-focused tests are present.
- Partial source and LLM failures degrade safely
  - Collector fault-injection and fallback-analysis tests are present.
- The monitoring agent exposes complete self-observability
  - Self-observability metrics, structured logs, and tracing configuration are implemented.
- Incident reports are available in JSON and Markdown
  - `IncidentReportRenderer` renders both formats and `IncidentController` exposes the JSON and Markdown endpoints.
- Risky production actions cannot run without explicit approval
  - Approval checks gate execution and approval workflow tests cover request/approve/reject behavior.
- Analysis, notifications, approvals, and actions are auditable
  - Audit service records redacted, encrypted events and the approval flow writes audit events.
- Unhealthy behavior can be detected with configurable policies
  - Anomaly detection tests cover threshold policies, service-specific overrides, and cooldown behavior.
- Related symptoms can be correlated into evidence-backed incidents
  - Correlation tests cover dependency chains, trace paths, deployment context, and severity aggregation.
- AI analysis produces valid structured output without unsupported claims
  - Incident analysis tests cover structured JSON parsing, fallback analysis, and prompt redaction.
- Severity is classified using health, customer impact, and blast radius
  - The analysis and correlation services propagate severity from health signals and multi-service impact.
- Alerts reach configured channels with required incident information
  - Notification tests verify Slack and email delivery payloads and unsupported-channel handling.

## Supported by Documentation and Operational Guidance

- Timing targets are documented and partially enforced through tunable configuration
- The application builds, deploys, and operates successfully on Linux is documented with Linux baselines, Compose guidance, and Helm guidance
- Security, performance, resilience, and recovery tests are documented and partly implemented
- Documentation and runbooks exist for release and operations, but final human approval is still a release-step responsibility

## Still Requires Environment Signoff

- Unhealthy behavior can be detected with configurable policies
- Related symptoms can be correlated into evidence-backed incidents
- AI analysis produces valid structured output without unsupported claims
- Severity is classified using health, customer impact, and blast radius
- Alerts reach configured channels with required incident information
- Incident reports are available in JSON and Markdown
- Risky production actions cannot run without explicit approval
- Analysis, notifications, approvals, and actions are auditable
- Timing targets are met under the expected workload
- Security, performance, resilience, and recovery tests pass
- Documentation and runbooks are approved for release

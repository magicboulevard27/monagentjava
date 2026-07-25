# Monagent Security Threat Model

## Scope

- Collectors ingest health, metrics, logs, traces, Kubernetes, and deployment context.
- The AI reasoning layer consumes normalized evidence and may call an external LLM.
- The REST API exposes incidents, recommendations, approvals, and reports.
- Notification channels send incident summaries to external systems.
- Audit records retain control-plane activity.

## Primary Threats

- SSRF from collector targets or notification callbacks.
- Prompt injection embedded in incident evidence or deployment text.
- Secret leakage through logs, audit payloads, notifications, or AI prompts.
- Unsafe deserialization from external JSON responses.
- Unauthorized access to incidents, approvals, or control actions.
- Replay, duplicate, or stale approval decisions.
- Execution of risky production actions without explicit policy.
- Integrity loss in audit or incident evidence records.

## Controls

- Restrict outbound URLs to trusted service targets.
- Redact secrets before AI requests, notifications, and audit writes.
- Require authenticated access and role-based authorization for API actions.
- Store only sanitized evidence payloads.
- Validate structured JSON output from the LLM.
- Require approval before risky recommendation execution.
- Default production write actions to disabled.
- Record audit events for requests, approvals, failures, and execution results.

## Residual Risks

- Local in-memory auth is a temporary development control and not a production identity provider.
- TLS enforcement for all external integrations depends on configured endpoints and deployment environment.
- The project currently relies on application-level controls for some threat classes and should be complemented with deployment hardening.

## Follow-Up

- Replace development auth with the selected external identity provider.
- Add deployment-time TLS validation for all configured endpoints.
- Add container and dependency scanning to CI if not already enforced by the pipeline.

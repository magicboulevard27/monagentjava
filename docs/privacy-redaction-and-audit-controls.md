# Privacy, Redaction, and Audit Controls

## Redaction

- Remove secrets, tokens, passwords, and credentials from logs, alerts, prompts, and incident evidence.
- Prefer stable identifiers such as incident IDs, correlation IDs, service names, and timestamps.
- Keep fixtures and examples sanitized.

## Privacy Boundaries

- Do not send unnecessary sensitive payloads to the LLM provider.
- Keep customer-specific or production-sensitive data out of example files.
- Store only the evidence needed for incident analysis and auditability.

## Audit Controls

- Record approval decisions, notification dispatches, and guarded execution attempts.
- Preserve enough context to explain what happened without exposing secrets.
- Treat audit logs as immutable operational records.

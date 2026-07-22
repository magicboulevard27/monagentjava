# ADR-0007: Use Slack and email for initial notifications

## Status

Accepted

## Context

The requirements list multiple possible channels, but the MVP only needs a small, high-value notification surface.

## Decision

Use Slack and email as the initial notification channels.

## Consequences

- Notification templates should be designed to render safely for chat and email.
- Integrations for Microsoft Teams, Jira, PagerDuty, and Opsgenie remain future extensions.


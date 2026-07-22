# ADR-0008: Use enterprise OIDC with role-based access control

## Status

Accepted

## Context

The system must secure API access, define roles, and protect monitoring data, prompts, and incident records.

## Decision

Use an enterprise OpenID Connect-compatible identity provider with role-based access control. Microsoft Entra ID is the default enterprise option for this project.

## Roles

- `viewer`
- `operator`
- `approver`
- `administrator`
- `service`

## Consequences

- Spring Security should enforce role checks at the API boundary.
- Service-to-service access must be treated separately from human user access.
- Environment-specific identity settings should be externalized.


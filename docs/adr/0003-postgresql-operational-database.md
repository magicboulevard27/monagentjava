# ADR-0003: Use PostgreSQL for operational persistence

## Status

Accepted

## Context

The design requires persistence for monitored services, signals, incidents, evidence, recommendations, approvals, and audit logs.

## Decision

Use PostgreSQL as the primary operational database.

## Consequences

- Schema design should use PostgreSQL-friendly constraints, indexes, and JSONB where appropriate.
- Integration testing should assume PostgreSQL semantics.


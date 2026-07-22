# ADR-0004: Use a modular monolith for the MVP

## Status

Accepted

## Context

The design describes multiple logical components, but the repo is at the initiation stage and the MVP needs a low-friction deployment and simpler local development story.

## Decision

Implement the MVP as one deployable Spring Boot application with clear package and module boundaries for API, domain, persistence, collection, analysis, AI, notification, approval, and audit components.

## Consequences

- Deployment and local setup stay simpler for the first release.
- Internal boundaries must be enforced in code to avoid a monolith becoming an unstructured codebase.
- The architecture can later be split into separate services if operational pressure justifies it.


# ADR-0005: Use a database-backed queue for asynchronous processing

## Status

Accepted

## Context

The system must schedule collectors and process analysis work asynchronously, but the MVP should avoid introducing broker infrastructure before the core workflow is proven.

## Decision

Use a database-backed queue for asynchronous processing in the MVP.

## Consequences

- Message persistence shares the operational database.
- The initial deployment remains smaller than a Kafka- or RabbitMQ-based design.
- Queue semantics must be designed carefully to avoid duplicate work and starvation.


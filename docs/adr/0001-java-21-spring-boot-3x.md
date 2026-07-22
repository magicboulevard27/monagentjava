# ADR-0001: Use Java 21 and Spring Boot 3.x

## Status

Accepted

## Context

The requirements and design specify a Java-based monitoring agent that must integrate with Spring Boot microservices and run reliably on Linux.

## Decision

Use Java 21 and Spring Boot 3.x as the application baseline.

## Consequences

- Access to a modern language level and current Spring ecosystem support.
- Alignment with the target runtime and framework expectations in the requirements.
- New code should avoid relying on pre-Java-21 language features unless needed for compatibility.


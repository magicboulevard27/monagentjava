# ADR-0002: Use Maven for build and dependency management

## Status

Accepted

## Context

The project needs a standard build system for Java 21, Spring Boot, testing, dependency management, and plugin-driven quality checks.

## Decision

Use Maven as the build and dependency-management tool.

## Consequences

- The initial project structure and CI expectations should follow Maven conventions.
- Build, test, and packaging tasks will be expressed as Maven goals.


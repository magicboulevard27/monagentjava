# Project Charter

## Purpose

Build an AI-powered microservices health monitoring agent that collects observability data, detects anomalies, correlates incidents, and generates evidence-based remediation guidance.

## MVP Scope

The first release will focus on:

- service registry and environment metadata
- Actuator health collection
- Prometheus metrics collection
- alert generation from deterministic rules
- initial incident evidence modeling
- AI-assisted incident summaries using a local LLM
- read-only notification delivery

The first release will not include:

- automated production remediation
- Kubernetes write actions
- multi-tenant support
- advanced incident learning
- non-approved external LLM providers

## Supported Environments

The application will be developed and validated for:

- development
- testing
- staging
- production

Runtime target:

- Linux

## Acceptance Criteria

The project is considered ready to move beyond initiation when:

- the technical baseline is documented
- the architecture decisions are captured as ADRs
- the MVP scope is agreed and traceable to the requirements
- the engineering working agreement is published

## Baseline Technology Decisions

- Language: Java 21
- Framework: Spring Boot 3.x
- Build tool: Maven
- Database: PostgreSQL
- LLM provider: Ollama
- Primary notification channels for MVP: Slack and email
- Auth provider: OpenID Connect-compatible enterprise identity provider, with Microsoft Entra ID as the default enterprise option


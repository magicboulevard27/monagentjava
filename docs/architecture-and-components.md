# Architecture and Components

## Overview

Monagent is a modular monolith Spring Boot application with four main runtime roles:

- `api`: exposes the management and incident APIs
- `collector`: gathers health, metrics, logs, traces, and deployment context
- `analysis`: correlates evidence and prepares incident guidance
- `notification`: dispatches alerts and follow-up actions

## Core Layers

- API layer: REST controllers, validation, security, and request shaping
- Domain layer: incidents, monitored services, approvals, recommendations, and audit events
- Persistence layer: JPA entities, repositories, and Flyway migrations
- Collection layer: health, metrics, logs, traces, Kubernetes, and deployment collectors
- Analysis layer: anomaly detection, correlation, and recommendation generation
- AI layer: prompt construction, structured-output parsing, and fallback analysis
- Notification layer: outbound messaging and alert formatting
- Approval layer: approval requests, policy checks, and guarded action execution
- Audit layer: immutable records for operational and security-relevant activity

## Runtime Dependencies

- PostgreSQL is the primary data store
- Ollama is the initial local model runtime
- Optional observability backends include Prometheus, OpenSearch, and Jaeger or Tempo
- Kubernetes metadata is collected through the in-cluster API when the Kubernetes collector is enabled

## Deployment Shapes

- Local development: Docker Compose with PostgreSQL and the application
- Observability extension: Docker Compose profiles for Prometheus, OpenSearch, and Jaeger
- AI extension: Docker Compose profile for Ollama
- Kubernetes: Helm chart with separate workloads for API, collector, analysis, and notification

## Design Notes

- The collector workload has the only namespace-scoped Kubernetes read permissions
- The API, analysis, and notification workloads use the shared service account without extra RBAC
- The chart defaults to rolling updates, resource limits, probes, HPA, PDB, and NetworkPolicy controls

# Design Specifications Document

## AI-Powered Microservices Health Monitoring Agent

## 1. Purpose

This document defines the technical design for an AI-powered monitoring agent that observes Java Spring Boot microservices, correlates health signals, detects incidents, and generates evidence-based operational guidance.

The design is derived from `specs/requirements.md` and translates the functional and non-functional requirements into implementable system components, data flows, integrations, and operational controls.

## 2. Design Goals

The system is designed to:

1. Continuously collect service health, metrics, logs, traces, and deployment context.
2. Detect abnormal service behavior using configurable rules and thresholds.
3. Correlate evidence across observability sources.
4. Generate structured incident analysis using an AI reasoning layer.
5. Notify engineering teams through configured channels.
6. Preserve human approval for risky production remediation actions.
7. Continue operating when one or more data sources are degraded.

## 3. System Context

The monitoring agent sits between observability platforms, runtime infrastructure, AI reasoning services, and incident response tools.

```text
Spring Boot Services
    -> Actuator health/info
    -> Prometheus metrics
    -> OpenSearch/ELK logs
    -> Jaeger/Tempo traces

Kubernetes / CI/CD
    -> Pod status
    -> Deployment events
    -> Configuration changes
    -> Rollout history

Monitoring Agent
    -> Collection
    -> Normalization
    -> Anomaly detection
    -> Correlation
    -> AI reasoning
    -> Alerting
    -> Incident reporting
    -> Approval workflow

Notification and Incident Systems
    -> Slack
    -> Email
    -> Microsoft Teams
    -> Jira
    -> PagerDuty / Opsgenie
```

## 4. High-Level Architecture

The system is organized into the following logical services:

| Component | Responsibility |
| --- | --- |
| Service Registry | Stores monitored service definitions, environments, health endpoints, and metadata. |
| Health Collector | Polls Spring Boot Actuator endpoints and classifies service status. |
| Metrics Collector | Queries Prometheus for JVM, HTTP, database, Kafka, and infrastructure metrics. |
| Log Analyzer | Retrieves recent logs from OpenSearch/ELK and extracts operational patterns. |
| Trace Analyzer | Queries Jaeger/Tempo for slow paths, failed spans, and downstream bottlenecks. |
| Deployment Context Collector | Reads Kubernetes and CI/CD events related to deployments, restarts, scaling, and config changes. |
| Signal Normalizer | Converts heterogeneous source data into a common monitoring signal model. |
| Anomaly Detection Engine | Evaluates signals against configurable thresholds and trend rules. |
| Correlation Engine | Groups related symptoms by service, time window, dependency path, and deployment context. |
| AI Reasoning Engine | Builds constrained prompts and asks the configured LLM to produce incident analysis. |
| Recommendation Engine | Converts detected causes and symptoms into remediation recommendations. |
| Notification Service | Sends alerts and reports to Slack, Email, Teams, Jira, PagerDuty, or Opsgenie. |
| Approval Workflow | Records user approvals and blocks risky actions until explicitly approved. |
| Audit Service | Records analysis, recommendations, approvals, alerts, and executed actions. |

## 5. Component Design

### 5.1 Service Registry

The Service Registry stores monitored service configuration.

Core fields:

```text
service_id
service_name
environment
health_url
prometheus_job_name
log_index_pattern
trace_service_name
kubernetes_namespace
kubernetes_workload_name
owner_team
alert_channels
enabled
```

The registry should support multiple environments such as development, testing, staging, and production.

### 5.2 Health Collector

The Health Collector polls:

```text
/actuator/health
/actuator/info
```

It maps raw results into:

```text
UP
DOWN
DEGRADED
UNKNOWN
```

`DEGRADED` should be used when a service is reachable but one or more dependent health contributors report a failure or warning.

### 5.3 Metrics Collector

The Metrics Collector queries Prometheus on a scheduled interval.

Required metric categories:

```text
CPU usage
Memory usage
JVM heap usage
GC pause time
Request rate
Error rate
Response latency
Database connection pool usage
Kafka consumer lag
Thread pool usage
```

Prometheus queries should be configurable per service because metric names and labels may differ across applications.

### 5.4 Log Analyzer

The Log Analyzer retrieves recent logs from OpenSearch or ELK using service name, environment, time window, and severity filters.

Pattern detection should cover:

```text
Exceptions
Timeouts
Connection refused errors
Database errors
Authentication failures
Retry exhaustion
Circuit breaker open events
OutOfMemoryError
```

Log records must be redacted before they are passed to AI reasoning.

### 5.5 Trace Analyzer

The Trace Analyzer reads distributed traces from Jaeger or Tempo.

It should detect:

```text
Slow request path
Failed downstream dependency
Service-to-service bottleneck
High latency span
External API delay
```

Trace evidence should include trace IDs, span names, duration, status, and dependency names.

### 5.6 Deployment Context Collector

The Deployment Context Collector reads Kubernetes and CI/CD events that happened near an incident window.

Relevant context:

```text
New deployment
Configuration change
Pod restart
Scaling event
Database migration
Infrastructure change
```

The collector should preserve timestamps so the Correlation Engine can determine whether symptoms started before or after a deployment event.

### 5.7 Anomaly Detection Engine

The Anomaly Detection Engine evaluates normalized signals against thresholds.

Default threshold examples:

| Condition | Default Threshold |
| --- | --- |
| Error rate | Greater than 5% |
| Latency p95 | Greater than 2 seconds |
| Memory usage | Greater than 85% |
| CPU usage | Greater than 80% |
| Service status | DOWN |
| Database pool usage | Greater than 90% |
| Kafka lag | Increasing continuously |

Thresholds must be configurable by service, environment, and severity policy.

### 5.8 Correlation Engine

The Correlation Engine groups anomalies into candidate incidents.

Correlation dimensions:

```text
Time window
Affected service
Downstream dependency
Upstream caller
Deployment event
Shared infrastructure component
Repeated log pattern
Trace path
```

The output is an evidence package used by the AI Reasoning Engine.

### 5.9 AI Reasoning Engine

The AI Reasoning Engine must analyze only provided data. It must not invent unsupported facts.

The prompt should include:

```text
System role
Task instructions
Normalized monitoring data
Detected anomalies
Relevant logs
Relevant traces
Deployment context
Output schema
Safety constraints
```

Supported providers:

```text
Ollama local LLM
Approved remote LLM provider
```

The engine should prefer structured JSON output internally and render Markdown summaries for humans.

### 5.10 Recommendation Engine

The Recommendation Engine maps incident evidence and AI analysis to practical remediation actions.

Recommended actions may include:

```text
Restart unhealthy pods
Scale service replicas
Check database connection pool
Rollback latest deployment
Inspect failed dependency
Increase resource limits
Check Kafka consumer lag
Review recent configuration changes
```

Each recommendation must include supporting evidence and a risk level.

### 5.11 Notification Service

The Notification Service sends incident alerts and reports to configured destinations.

Supported channels:

```text
Slack
Email
Microsoft Teams
Jira
PagerDuty
Opsgenie
```

Notifications should include incident ID, severity, affected services, symptoms, likely root cause, confidence, and recommended next steps.

### 5.12 Approval Workflow

Risky production actions require explicit human approval.

Approval-required actions:

```text
Rollback deployment
Restart production pods
Scale production workloads
Change configuration
Trigger failover
```

Safe automated actions may run without approval when enabled:

```text
Create Jira ticket
Send alert
Collect diagnostic bundle
Generate incident summary
Trigger read-only Kubernetes inspection
```

## 6. Data Model

The system should persist operational records required for auditability, incident history, and troubleshooting.

### 6.1 Monitored Services

```text
monitored_services
- service_id
- service_name
- environment
- owner_team
- health_url
- prometheus_job_name
- log_index_pattern
- trace_service_name
- kubernetes_namespace
- kubernetes_workload_name
- enabled
- created_at
- updated_at
```

### 6.2 Monitoring Signals

```text
monitoring_signals
- signal_id
- service_id
- source_type
- signal_name
- signal_value
- unit
- status
- severity
- collected_at
- raw_reference
```

### 6.3 Incidents

```text
incidents
- incident_id
- title
- severity
- status
- affected_services
- start_time
- detected_at
- resolved_at
- likely_root_cause
- confidence
- summary
- created_at
- updated_at
```

### 6.4 Incident Evidence

```text
incident_evidence
- evidence_id
- incident_id
- source_type
- service_name
- evidence_type
- description
- observed_at
- reference_id
- redacted_payload
```

### 6.5 Recommendations

```text
recommendations
- recommendation_id
- incident_id
- action_type
- description
- risk_level
- requires_approval
- status
- evidence_summary
- created_at
```

### 6.6 Approvals and Audit Logs

```text
approvals
- approval_id
- recommendation_id
- requested_by
- approved_by
- approval_status
- decision_reason
- decided_at

audit_logs
- audit_id
- actor
- action
- entity_type
- entity_id
- event_payload
- created_at
```

## 7. Core Data Flow

1. The Service Registry provides enabled services and environment metadata.
2. Collectors gather health, metrics, logs, traces, and deployment context.
3. The Signal Normalizer converts raw data into standard monitoring signals.
4. The Anomaly Detection Engine flags abnormal behavior.
5. The Correlation Engine groups anomalies into incident candidates.
6. The AI Reasoning Engine generates explanation, severity, root cause, confidence, and recommendations.
7. The Notification Service sends alerts and incident reports.
8. The Approval Workflow gates risky actions.
9. The Audit Service records analysis, notifications, approvals, and executed actions.

## 8. API Design

### 8.1 Service Registry APIs

```text
GET    /api/v1/services
POST   /api/v1/services
GET    /api/v1/services/{serviceId}
PUT    /api/v1/services/{serviceId}
DELETE /api/v1/services/{serviceId}
```

### 8.2 Incident APIs

```text
GET  /api/v1/incidents
POST /api/v1/incidents/analyze
GET  /api/v1/incidents/{incidentId}
GET  /api/v1/incidents/{incidentId}/evidence
GET  /api/v1/incidents/{incidentId}/recommendations
```

### 8.3 Approval APIs

```text
POST /api/v1/recommendations/{recommendationId}/approve
POST /api/v1/recommendations/{recommendationId}/reject
GET  /api/v1/approvals
```

### 8.4 Reporting APIs

```text
GET /api/v1/reports/incidents/{incidentId}
GET /api/v1/reports/incidents/{incidentId}/markdown
GET /api/v1/reports/incidents/{incidentId}/json
```

## 9. AI Output Contract

The AI Reasoning Engine should return structured output compatible with the required incident format.

```json
{
  "incidentId": "INC-2026-001",
  "severity": "CRITICAL",
  "affectedServices": ["order-service", "payment-service"],
  "currentStatus": "Payment Service is degraded. Order Service latency is increasing.",
  "symptoms": [
    "Payment Service error rate is 42%",
    "Order Service p95 latency is 5.2 seconds"
  ],
  "evidence": [
    "Logs show database connection timeout",
    "Issue started 8 minutes after latest Payment Service deployment"
  ],
  "likelyRootCause": "Payment Service database connection pool configuration may be incorrect after the latest deployment.",
  "confidence": "HIGH",
  "recommendedActions": [
    "Review recent Payment Service deployment",
    "Check database connection pool settings",
    "Roll back Payment Service if configuration changed"
  ],
  "escalationRequired": true
}
```

## 10. Severity Classification

Severity classification uses service health, customer impact, blast radius, and availability.

| Severity | Criteria |
| --- | --- |
| LOW | Minor anomaly, no customer impact, service remains healthy. |
| MEDIUM | Degraded behavior or localized issue affecting one service. |
| HIGH | Significant error rate, latency, or dependency failure affecting user workflows. |
| CRITICAL | Service outage, widespread customer impact, failed core workflow, or cascading failure. |

## 11. Security Design

The system must protect observability data, credentials, tokens, prompts, responses, and incident reports.

Security controls:

```text
Authenticated API access
Role-based authorization
Secret storage through environment-specific secret managers
Read-only credentials for observability integrations where possible
Kubernetes RBAC with least privilege
Prompt and response redaction
Audit logging for sensitive actions
TLS for service-to-service traffic
```

Sensitive data must be masked before it reaches the AI Reasoning Engine.

Examples:

```text
Access tokens
Passwords
Authorization headers
Customer identifiers
Email addresses
Payment data
Session cookies
Database connection strings
```

## 12. Reliability Design

The agent must tolerate partial data-source failure.

Reliability behavior:

```text
If health endpoints fail, use metrics and Kubernetes status when available.
If tracing is unavailable, continue analysis with metrics, logs, and deployment context.
If logs are unavailable, continue detection using health, metrics, and traces.
If the LLM is unavailable, still generate rule-based alerts and evidence bundles.
If notification delivery fails, retry and record audit failure events.
```

## 13. Performance Design

The system should meet the following timing targets:

| Operation | Target |
| --- | --- |
| Health check delay | Less than 30 seconds |
| Alert generation | Less than 60 seconds after detection |
| Incident summary generation | Less than 2 minutes |

Collectors should run asynchronously so slow integrations do not block unrelated data collection.

## 14. Deployment Design

The recommended deployment target is Kubernetes.

Deployment units:

```text
monitoring-agent-api
collector-worker
analysis-worker
notification-worker
postgresql or managed relational database
optional message broker
optional local Ollama runtime
```

The design should support Docker Compose for local development and Kubernetes manifests or Helm charts for shared environments.

## 15. Observability for the Agent

The monitoring agent must expose its own health and telemetry.

Required self-observability:

```text
/actuator/health
/actuator/info
Collector success and failure counts
Source query latency
AI reasoning latency
Notification delivery status
Incident analysis count
Approval decision count
Error logs with correlation IDs
```

## 16. Acceptance Mapping

| Acceptance Criterion | Design Coverage |
| --- | --- |
| Discover and monitor registered microservices | Service Registry, Health Collector |
| Collect health, metrics, logs, and traces | Health, Metrics, Log, and Trace Collectors |
| Detect unhealthy service behavior | Anomaly Detection Engine |
| Generate meaningful root cause analysis | Correlation Engine and AI Reasoning Engine |
| Classify severity correctly | Severity Classification |
| Send alerts to configured channels | Notification Service |
| Generate incident reports | Reporting APIs and AI Output Contract |
| Protect sensitive data before AI analysis | Security Design and redaction controls |
| Require approval before risky production actions | Approval Workflow |
| Maintain audit logs | Audit Service and audit data model |

## 17. Implementation Phases

### Phase 1: Basic Monitoring Agent

```text
Service registry
Actuator health collection
Prometheus metrics collection
Basic threshold detection
Alert notification
```

### Phase 2: Log and Trace Correlation

```text
OpenSearch/ELK integration
Jaeger/Tempo integration
Log pattern detection
Trace bottleneck detection
Incident evidence model
```

### Phase 3: AI Reasoning Layer

```text
Prompt builder
LLM provider abstraction
Structured AI output parser
Incident summary generation
Root cause and confidence scoring
```

### Phase 4: Incident Automation

```text
Jira ticket creation
Slack and Teams incident reports
Approval workflow
Audit logging
Diagnostic bundle collection
```

### Phase 5: Advanced Remediation

```text
Read-only Kubernetes inspection
Restart and scale recommendations
Rollback recommendations
Human-approved execution path
Historical incident learning
```

## 18. Open Design Decisions

The following decisions should be finalized before implementation:

1. Primary backend language and framework.
2. Primary database for incident and audit persistence.
3. Whether collection runs in-process or through separate workers.
4. Whether asynchronous processing requires Kafka, RabbitMQ, or a lighter queue.
5. Supported LLM provider for the first release.
6. Initial notification channels for MVP.
7. Required authentication provider and role model.

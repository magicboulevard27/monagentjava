# Testing and Quality Assurance Baseline

## Coverage Targets

- Domain and business logic: minimum 45 percent instruction coverage enforced in Maven.
- Critical workflow paths should have direct tests for detection, correlation, analysis, recommendation, notification, and approval.

## Test Layers

- Unit tests cover thresholds, redaction, parsing, mapping, and policy decisions.
- Integration tests cover database persistence and Spring wiring.
- Pipeline tests exercise the in-memory flow across collection, analysis, correlation, recommendation, and notification.

## Release-Blocking Defects

- Do not release with failing unit, integration, or pipeline tests.
- Do not release with broken security or authorization tests.
- Do not release if coverage drops below the enforced baseline.

## Sanitized Fixtures

- Keep fixtures free of secrets, tokens, credentials, and real customer data.
- Reuse the same sanitized metrics, log, trace, and Kubernetes samples across regression tests.

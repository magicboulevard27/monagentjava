# API and OpenAPI

The OpenAPI specification lives in [`specs/openapi.yaml`](/Z:/_Dev/AI-agents/monagentjava/specs/openapi.yaml).

## What It Covers

- Monitored service registration and management
- Incident listing and retrieval
- Approval workflow endpoints
- Health and operational endpoints for the application

## Usage Examples

Create a monitored service:

```bash
curl -X POST http://localhost:8080/api/v1/services \
  -H 'Content-Type: application/json' \
  -d '{"serviceName":"orders-api","environment":"production"}'
```

List incidents:

```bash
curl http://localhost:8080/api/v1/incidents
```

Check health:

```bash
curl http://localhost:8080/actuator/health
```

## Compatibility Notes

- Keep request and response schemas aligned with the OpenAPI contract
- Preserve existing fields when extending responses
- Prefer additive changes over breaking contract changes
- Update example payloads when new required fields or enum values are introduced

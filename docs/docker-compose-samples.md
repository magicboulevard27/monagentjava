# Docker Compose Sample Configuration

## Service Registration Example

```json
{
  "serviceName": "orders-api",
  "environment": "development",
  "healthUrl": "http://orders-api:8080/actuator/health",
  "prometheusJobName": "orders-api",
  "prometheusBaseUrl": "http://orders-api:8080",
  "logSearchEndpoint": "http://opensearch:9200/orders-api-*/_search",
  "traceServiceName": "orders-api",
  "kubernetesNamespace": "default",
  "kubernetesWorkloadName": "orders-api",
  "ownerTeam": "platform",
  "alertChannels": ["slack", "email"],
  "enabled": true
}
```

## Threshold Example

```json
{
  "serviceName": "orders-api",
  "environment": "development",
  "metricName": "cpu",
  "comparison": "GREATER_THAN",
  "thresholdValue": 80,
  "severity": "HIGH",
  "evaluationWindowMinutes": 5,
  "minimumSampleSize": 3,
  "cooldownMinutes": 10
}
```

## Local Defaults

- Use the sample registration against the local Compose services.
- Start with conservative thresholds and tighten them after observing real traffic.
- Keep the local LLM and observability services disabled unless you are actively validating those integrations.

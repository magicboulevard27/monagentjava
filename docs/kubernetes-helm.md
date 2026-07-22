# Kubernetes and Helm Deployment

## Deployment Model

- The chart deploys the application as a modular monolith with four workload-specific deployments:
  - API
  - collector worker
  - analysis worker
  - notification worker
- Each workload uses the same release image and different runtime labels and environment settings.

## Security and Access

- Use a dedicated service account created by the chart.
- Use ConfigMap and Secret references for runtime configuration.
- Keep database and Ollama values out of plain-text manifests when deploying for real environments.
- Apply the chart’s default NetworkPolicy to limit traffic to in-namespace peers and required egress.

## Probes and Scaling

- Startup, readiness, and liveness probes are configured for the API workload.
- HPA is enabled for the API and worker deployments.
- Pod disruption budgets and topology spread constraints are enabled by default for the API workload.

## Optional Ollama

- Set `ollama.enabled=true` when you want the model runtime deployed with the application.
- Increase CPU and memory if the selected model requires more headroom.
- Keep Ollama out of production clusters unless the operational model and capacity are explicitly approved.

## Rollout and Recovery

- Use standard Kubernetes rolling updates with the chart defaults.
- Roll back by reverting to the previous chart release if health checks fail after upgrade.
- Validate failover by restarting pods and confirming the service returns to ready state without data loss.
- Validate disaster recovery by restoring the database and redeploying the chart into a clean namespace.

# Deployment Guide

## Linux

- Install Java 21 and the required host packages listed in `docs/linux-baseline.md`.
- Configure environment variables and mounted secrets from `docs/configuration-and-secrets.md`.
- Use the provided `systemd` unit when running outside containers.

## Docker Compose

- Use `docker compose up --build` for the base stack.
- Add `--profile observability` for Prometheus, OpenSearch, and Jaeger.
- Add `--profile ai` for Ollama.
- Follow `docs/docker-compose.md` for verification and recovery steps.

## Kubernetes and Helm

- Use the Helm chart in `helm/monagentjava`.
- Keep the API, collector, analysis, and notification workloads on the same release image.
- Follow `docs/kubernetes-helm.md` for rollout and recovery expectations.

## Release Hygiene

- Validate the release process in `docs/release-process.md`.
- Verify the chart and Compose contracts before promotion.
- Promote the same tagged artifact across environments.

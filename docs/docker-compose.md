# Docker Compose Local Deployment

## Services

- `postgres`: PostgreSQL for application data.
- `monagent`: the Spring Boot application.
- `prometheus`: optional metrics backend, enabled with the `observability` profile.
- `opensearch`: optional log backend, enabled with the `observability` profile. It requires `OPENSEARCH_INITIAL_ADMIN_PASSWORD` for local startup; the Compose file now defaults this to `admin123!`.
- `jaeger`: optional tracing backend, enabled with the `observability` profile.
- `ollama`: optional local LLM backend, enabled with the `ai` profile.

## Startup

1. Build the application JAR with Maven.
2. Start the base stack with `docker compose up --build`.
3. Add observability services with `docker compose --profile observability up --build`.
4. Add the local LLM with `docker compose --profile ai up --build`.

## Verification

1. Validate the Compose file with `docker compose config`.
2. Start the stack with `docker compose up --build`.
3. Run `./scripts/verify-compose.sh` once the app is healthy.
4. If you want a single command for the script path, use `BASE_URL=http://127.0.0.1:8080 ./scripts/verify-compose.sh`.
5. On Windows, run `.\scripts\verify-compose.ps1` against the same base URL.

## Shutdown

- Stop the stack with `docker compose down`.
- Remove persistent volumes with `docker compose down -v` only when you want a full reset.

## Reset

- Delete the Docker volumes if you need a clean database, metrics store, or Ollama cache.
- Re-run the stack after cleaning volumes to recreate a fresh local environment.

## Troubleshooting

- If the app fails health checks, inspect `docker compose logs monagent`.
- If PostgreSQL does not initialize, inspect `docker compose logs postgres`.
- If the app cannot connect to Ollama, verify that the `ai` profile is enabled and the model is available.
- If Prometheus or Jaeger are enabled, verify that their profile-specific ports are not already in use.
- If Docker reports a missing network ID, run `docker compose down`, then `docker network rm monagent-net` if it still exists, and start again with `docker compose up --build`.
- If the issue persists after a stale network cleanup, run `docker compose down -v --remove-orphans` and recreate the stack from scratch.
- If `docker compose up` fails with `failed to set up container networking` or a missing network ID, stop the stack, remove any stale project network, and rerun `docker compose up --build`.
- If the app is up but verification fails, run `./scripts/health-check.sh` against the same `BASE_URL` before retrying the Compose flow.

## Sample Local Configuration

- `config/monagent-local.properties` provides the reload file used by the app container.
- Environment variables in `docker-compose.yml` provide the database, queue, and LLM defaults for local use.

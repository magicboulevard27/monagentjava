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

## Sample Local Configuration

- `config/monagent-local.properties` provides the reload file used by the app container.
- Environment variables in `docker-compose.yml` provide the database, queue, and LLM defaults for local use.

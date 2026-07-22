# Configuration and Secrets

## Configuration Model

Configuration is exposed through typed Spring Boot properties under the `monagent` namespace.

## Sources

- Environment variables for normal deployment overrides
- Mounted secret files through Spring `configtree` imports
- Profile-specific YAML for local, test, staging, production, and Linux runtime behavior

## Secrets Handling

Secrets should be supplied through one of the following:

- environment variables
- mounted secret files in `/etc/monagent/secrets/`
- mounted secret files in `/run/secrets/`

Secrets must not be logged or returned in API responses.

## Required Variables

- `MONAGENT_DATASOURCE_URL`
- `MONAGENT_DATASOURCE_USERNAME`
- `MONAGENT_DATASOURCE_PASSWORD`
- `MONAGENT_OLLAMA_BASE_URL`
- `MONAGENT_OLLAMA_MODEL`
- `MONAGENT_AUTH_ISSUER_URI`
- `MONAGENT_AUTH_AUDIENCE`

## Sample Values

- `MONAGENT_DATASOURCE_URL=jdbc:postgresql://localhost:5432/monagent`
- `MONAGENT_OLLAMA_BASE_URL=http://localhost:11434`
- `MONAGENT_OLLAMA_MODEL=llama3.1:8b-instruct`
- `MONAGENT_AUTH_ISSUER_URI=https://login.microsoftonline.com/common/v2.0`

## Production Defaults

- use `UTC`
- use explicit production profile overrides
- require externalized secrets for database and identity settings


# Configuration, Secrets, and Certificates

## Configuration Sources

- Environment variables
- Spring profile-specific YAML
- Mounted config tree secrets from `/etc/monagent/secrets/` and `/run/secrets/`

## Required Secrets

- Database URL, username, and password
- Authentication issuer URI and audience
- Ollama endpoint and model selection when AI analysis is enabled
- Optional observability credentials where a backend requires them

## Handling Rules

- Do not log secrets
- Do not echo secrets in API responses or diagnostics
- Prefer mounted secret files over plain-text values for production
- Use profile-specific overrides for local, test, staging, and production behavior

## Certificate Guidance

- Use TLS for all external and service-to-service connections
- Rotate certificates before expiration
- Verify endpoint certificates when introducing a new integration
- Keep CA bundle and truststore material consistent with the deployment environment

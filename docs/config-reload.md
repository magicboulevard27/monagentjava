# Configuration Reload

The application supports a constrained reload path for runtime-safe configuration.

## Supported Reloads

- runtime timezone
- runtime log directory
- runtime data directory
- Ollama model selection
- enabled notification channels

## Reload Mechanism

- Put override values in the configured reload file.
- Default file: `/etc/monagent/monagent-reload.properties`
- Trigger reload through the actuator `configreload` endpoint.

## Not Reloaded

The following settings are not hot-reloaded and still require restart or redeployment:

- database connection credentials
- security provider configuration
- core Spring infrastructure

## Safety Note

Reload is intentionally limited to settings that can be refreshed without tearing down the application context.


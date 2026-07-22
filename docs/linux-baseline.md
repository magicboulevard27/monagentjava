# Linux Development and Runtime Baseline

## Supported Distributions

The application is intended to run on current long-term support Linux distributions, including:

- Ubuntu Server LTS
- Debian stable
- Red Hat Enterprise Linux 9 and compatible derivatives

## Required Packages

- OpenJDK 21 or a compatible Java 21 runtime
- Maven 3.9+ for local builds
- `curl` for health checks
- `tar` and standard GNU core utilities for packaging and operational scripts
- `systemd` for host-managed deployments

## Service Account

Run the application as a dedicated non-root service account named `monagent`.

## Directory Conventions

- Application root: `/opt/monagent`
- Configuration: `/etc/monagent`
- Logs: `/var/log/monagent`
- Data: `/var/lib/monagent`
- Runtime files: `/run/monagent`

## File Permissions

- Configuration files should be owned by `root:monagent` or `monagent:monagent` depending on deployment model.
- Writable data and log directories should be owned by `monagent`.
- Secrets must not be world-readable.

## JVM and Process Baseline

- Use Java 21 container-aware defaults.
- Prefer G1GC unless a later performance review justifies a different collector.
- Set heap sizing explicitly per environment.
- Use UTC for internal timestamps.
- Allow environment-specific reporting time zones only at presentation boundaries.

## Shutdown and Signals

- Handle `SIGTERM` for graceful shutdown in `systemd` and container environments.
- Handle `SIGINT` for interactive local execution.
- Allow a bounded termination window so in-flight work can stop cleanly.

## Host Deployment Notes

- Use `systemd` for non-containerized service management.
- Use log rotation for host-based deployment.
- Keep startup and diagnostics scripts in the repository for repeatable operations.

## Rootless Operation

The application should be installable and operable without root privileges when deployed into a user-owned directory tree or managed service account environment.


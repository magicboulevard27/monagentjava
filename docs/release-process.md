# Release Process

## Versioning

- Use semantic versioning: `MAJOR.MINOR.PATCH`.
- Reserve pre-release tags such as `-alpha`, `-beta`, or `-rc.N` for non-production validation.
- Tag release candidates as `vX.Y.Z` in Git so CI can package the release artifact.

## Release Notes

- Summarize user-visible changes, configuration changes, migration steps, and known limitations.
- Call out any new operational requirements, especially database, notification, or observability changes.
- Include rollback notes when a release changes schema or runtime behavior.

## Rollback Criteria

- Roll back if startup fails, health checks do not go green, or a regression affects data integrity.
- Roll back if incident creation, notification delivery, or approval flow becomes unreliable.
- Roll back if a migration cannot be completed safely or if the application cannot recover cleanly after restart.

## Release Artifacts

- Application JAR produced by Maven.
- CycloneDX SBOM generated during package.
- Container image built from the release JAR.

## Promotion

- Build and verify on pull requests before merge.
- Tag a commit only after the release candidate has passed verification in the target environment.
- Promote the same tagged artifact across environments without rebuilding it.

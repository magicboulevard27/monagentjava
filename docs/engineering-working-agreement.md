# Engineering Working Agreement

## Coding Standards

- Follow standard Java 21 conventions.
- Use clear package boundaries and small, focused classes.
- Prefer explicit validation and immutable data structures where practical.
- Keep logging structured and free of secrets or raw sensitive payloads.

## Branching Strategy

- Use short-lived feature branches.
- Keep `main` in a releasable state.
- Merge only after review and verification.

## Versioning

- Use semantic versioning: `MAJOR.MINOR.PATCH`.
- Increment `PATCH` for bug fixes, `MINOR` for backward-compatible feature additions, and `MAJOR` for incompatible changes.

## Definition of Done

A task is done only when:

- the change satisfies the documented requirement
- the relevant tests or verification steps pass
- the change is documented if it introduces a new decision or operational requirement
- sensitive data handling is considered
- the final review identifies any remaining risk or follow-up work


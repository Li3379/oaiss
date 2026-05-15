<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# src — Source Root

Source code root containing main application code and test suites.

## Structure

| Directory | Purpose |
|-----------|---------|
| `main/` | Production source code (see main/AGENTS.md) |
| `test/` | Test suites (see test/AGENTS.md) |

## Conventions

- `main/` mirrors the standard Maven/Gradle layout: `main/java/` for sources, `main/resources/` for config.
- `test/` mirrors `main/` structure: test classes live in the same package path under `test/java/`.

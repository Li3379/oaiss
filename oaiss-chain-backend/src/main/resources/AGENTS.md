<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# src/main/resources — Application Configuration

Application configuration files, database migrations, and i18n resources.

## Key Files

| File | Purpose |
|------|---------|
| `application.yml` | Main Spring Boot config (port 8080, context /api/v1) |
| `application-dev.yml` | Dev profile overrides |
| `application-docker.yml` | Docker profile (MySQL, Redis, MinIO hostnames) |
| `application-local.yml` | Local dev profile |
| `application-test.yml` | Test profile (H2 in-memory DB) |
| `logback-spring.xml` | Logging config (JSON + console, ELK-ready) |

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `db/` | Flyway database migrations (see db/AGENTS.md) |
| `i18n/` | Internationalization message bundles (`messages_en_US.properties`, `messages_zh_CN.properties`) |

## For AI Agents

### Working
- Modify `application.yml` for default config; use profile-specific files for environment overrides.
- DB migrations go in `db/migration/` with Flyway naming (`V{N}__description.sql`).

### Testing
- Use `application-test.yml` for test runs (H2 in-memory). Never modify prod config in tests.

### Patterns
- Spring Boot multi-profile pattern. Config overrides by active profile.

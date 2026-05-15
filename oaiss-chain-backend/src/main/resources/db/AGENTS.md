<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# src/main/resources/db — Database Migrations

Database initialization and migration scripts managed by Flyway.

## Key Files

| File | Purpose |
|------|---------|
| `data.sql` | Reference data insertions |

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `migration/` | Flyway versioned migrations |

## Existing Migrations

- `V1__init_schema.sql` — Initial database schema
- `V2__seed_data.sql` — Seed/reference data
- `V3__test_seed_data.sql` — Test-specific seed data

## For AI Agents

- **Naming**: New migrations must follow `V{N}__description.sql` naming convention.
- **Immutability**: Never modify existing migration files — always add new ones.
- **Production**: Use validate-only mode in prod environments.

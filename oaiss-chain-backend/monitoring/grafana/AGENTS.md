<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-19 -->

# Grafana Dashboard Definitions

Grafana dashboard provisioning and JSON configuration files.

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `dashboards/` | Dashboard JSON configs and provisioning YAML |

## Conventions for AI Agents

- Dashboard JSON files follow Grafana's standard dashboard model schema.
- Provisioning is handled via `dashboards/dashboards.yml` — do not import dashboards manually in production.
- When adding a new dashboard, create the JSON file in `dashboards/` and register it in `dashboards.yml`.

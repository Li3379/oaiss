<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# Grafana Dashboard JSON Configurations

Dashboard provisioning config and pre-built dashboard definitions for OAISS Chain backend monitoring.

## Key Files

| File | Purpose |
|------|---------|
| `dashboards.yml` | Dashboard provisioning config (tells Grafana where to load dashboards from) |
| `oaiss-chain-backend.json` | Backend dashboard — JVM memory/threads, HTTP request metrics, database connection pool, Spring Boot actuator endpoints |

## Conventions for AI Agents

- `dashboards.yml` uses Grafana's file-based provisioning provider.
- Dashboard JSON uses Grafana's native dashboard model (panels, templating, datasources).
- The backend dashboard expects a Prometheus datasource named `Prometheus` and Spring Boot Micrometer metrics exposed at `/actuator/prometheus`.
- Metric naming follows Micrometer conventions: `jvm_memory_used`, `http_server_requests_seconds`, `hikaricp_connections_active`, etc.

<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-19 -->

# Operational Documentation

Production operations runbooks and procedures for OAISS Chain backend.

## Key Files

| File | Purpose |
|------|---------|
| `OPERATIONS.md` | Production operations runbook — deployment procedures, health checks, troubleshooting, rollback, and incident response |

## Conventions for AI Agents

- This directory is for operational runbooks consumed by on-call engineers and SREs.
- Keep documentation actionable: commands to run, logs to check, thresholds to monitor.
- When updating operational docs, verify commands against the current `docker-compose.yml` and `application.yml` configuration.

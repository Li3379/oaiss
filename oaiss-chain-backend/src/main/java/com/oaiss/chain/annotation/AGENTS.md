<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# com.oaiss.chain.annotation

Custom annotation definitions for cross-cutting concerns.

## Key Files

- `AuditLog.java` — Marks methods for audit logging
- `RateLimit.java` — Rate limiting annotation (requests per time window)
- `DistributedLock.java` — Redis distributed lock
- `DataIsolation.java` — Tenant data isolation
- `RequirePermission.java` — Permission check annotation

## For AI Agents

These annotations are processed by aspects in the `aop/` package. To add new cross-cutting behavior: define annotation here, implement aspect in `aop/`.

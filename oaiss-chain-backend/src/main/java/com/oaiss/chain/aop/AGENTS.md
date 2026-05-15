<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# com.oaiss.chain.aop

AOP aspect implementations for cross-cutting concerns.

## Key Files

- `AuditLogAspect.java` — Logs operations annotated with `@AuditLog`
- `RateLimitAspect.java` — Enforces rate limits via Redis
- `DistributedLockAspect.java` — Redis-based distributed locking
- `PermissionAspect.java` — Permission verification
- `DataIsolationAspect.java` — Multi-tenant data isolation

## For AI Agents

Aspects read annotations from the `annotation/` package. Each aspect intercepts method calls and applies the corresponding behavior (logging, rate limiting, locking, etc.).

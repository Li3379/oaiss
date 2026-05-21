<!-- Parent: ../AGENTS.md -->

# AOP Directory — OAISS Chain

> Cross-cutting concerns implemented as Spring AOP aspects. Each aspect corresponds to a custom annotation in `annotation/` and provides declarative infrastructure (logging, rate limiting, data isolation, distributed locking, permission checks) to service-layer methods.

Generated: 2026-05-19 | Updated: 2026-05-19

---

## Key Files

| File | Annotation | Mechanism | Key Dependencies |
|------|-----------|-----------|-----------------|
| `AuditLogAspect.java` | `@AuditLog` | AOP `@AfterReturning` / `@AfterThrowing` | `AuditLog` entity, `AuditLogRepository` |
| `RateLimitAspect.java` | `@RateLimit` | Redis token-bucket algorithm | `RedisTemplate`, `StringRedisTemplate` |
| `DataIsolationAspect.java` | `@DataIsolation` | ThreadLocal tenant context injection | `EnterpriseContextHolder` (security/) |
| `DistributedLockAspect.java` | `@DistributedLock` | Redis `SET NX EX` lock acquisition | `RedisTemplate`, `Lua` unlock script |
| `PermissionAspect.java` | `@RequirePermission` | Role + permission validation | `SecurityUtils` (security/), `UserTypeEnum` (enums/) |

---

## Aspect Details

### AuditLogAspect
Intercepts methods annotated with `@AuditLog`. Captures:
- Operator ID and username (from `SecurityUtils`)
- HTTP method and request URI (from `RequestContextHolder`)
- Method name and arguments
- Execution result or exception
- Timestamp

Writes audit records to the `audit_log` table via `AuditLogRepository`. Fires on both success (`@AfterReturning`) and failure (`@AfterThrowing`).

### RateLimitAspect
Implements request throttling via Redis token-bucket. The `@RateLimit` annotation specifies:
- `key` — Rate limit key prefix (e.g., `"report_submit"`)
- `count` — Maximum requests per time window
- `period` — Time window in seconds

On limit exceeded, throws `RateLimitException` (mapped to HTTP 429 by global exception handler). Uses Redis `INCR` + `EXPIRE` for atomic counting.

### DataIsolationAspect
Enforces multi-tenant data isolation by injecting the current enterprise ID into a ThreadLocal context (`EnterpriseContextHolder`). The aspect:
1. Reads the enterprise ID from the authenticated user's JWT claims
2. Sets it on `EnterpriseContextHolder`
3. Clears the ThreadLocal in a `@After` advice to prevent leaks

Repository queries use `...AndDeletedFalseAndEnterpriseId()` patterns that read from this context.

### DistributedLockAspect
Provides Redis-based distributed locking for concurrent operations (e.g., trade execution, auction matching). The `@DistributedLock` annotation specifies:
- `key` — Lock key (supports SpEL expressions like `#reportId`)
- `waitTime` — Maximum wait to acquire lock (ms)
- `leaseTime` — Lock auto-expiry (ms)

Implements lock with Redis `SET key value NX EX leaseTime`. Unlock uses a Lua script to ensure only the lock holder releases it. On acquisition failure, throws `DistributedLockException`.

### PermissionAspect
Fine-grained permission checking beyond Spring Security's role-based access. The `@RequirePermission` annotation specifies:
- `value` — Required permission string (e.g., `"report:audit"`, `"trade:create"`)

Validates that the current user (from `SecurityUtils.getCurrentUser()`) holds the required permission. Throws `AccessDeniedException` on failure.

---

## Cross-References

| Aspect | Paired Annotation | Related Components |
|--------|------------------|-------------------|
| `AuditLogAspect` | `annotation/AuditLog.java` | `entity/AuditLog`, `repository/AuditLogRepository` |
| `RateLimitAspect` | `annotation/RateLimit.java` | `config/RedisConfig`, `exception/RateLimitException` |
| `DataIsolationAspect` | `annotation/DataIsolation.java` | `security/EnterpriseContextHolder`, repository `enterpriseId` filters |
| `DistributedLockAspect` | `annotation/DistributedLock.java` | `config/RedisConfig`, `exception/DistributedLockException` |
| `PermissionAspect` | `annotation/RequirePermission.java` | `security/SecurityUtils`, `enums/UserTypeEnum` |

---

## AI Agent Notes

- **Adding a new aspect**: Create the annotation in `annotation/`, then the matching aspect here. Register the aspect as a Spring `@Component`. Follow the naming convention: `XxxAspect.java` paired with `@Xxx` annotation.
- **Aspect execution order**: Use `@Order` on aspects when multiple aspects apply to the same method. Recommended order: `@DistributedLock` (outermost) > `@RateLimit` > `@DataIsolation` > `@RequirePermission` > `@AuditLog` (innermost).
- **ThreadLocal cleanup**: Any aspect that sets ThreadLocal context (e.g., `DataIsolationAspect`) must clear it in `@After` advice. Missing cleanup causes data leaks across requests in thread-pool environments.
- **SpEL in annotations**: `@DistributedLock#key` supports SpEL expressions. Use `#paramName` to reference method parameters for per-resource locking.
- **Redis dependency**: `RateLimitAspect` and `DistributedLockAspect` require a running Redis instance. They will throw at startup if `RedisTemplate` is not configured.

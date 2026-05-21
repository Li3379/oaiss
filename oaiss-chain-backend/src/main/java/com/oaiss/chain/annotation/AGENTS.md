<!-- Parent: ../AGENTS.md -->
# Annotation Directory — Cross-Cutting Custom Annotations

> Custom annotation definitions that drive AOP-based cross-cutting concerns.
> Each annotation is paired with a corresponding aspect in `../aop/`.

Generated: 2026-05-19 | Updated: 2026-05-19

## Key Files

| File | Purpose | Used By |
|------|---------|---------|
| `AuditLog.java` | Marks methods for audit-log recording; attributes: `operation`, `description` | `aop/AuditLogAspect` |
| `RateLimit.java` | Marks methods for QPS-based rate limiting; attributes: `qps`, `message` | `aop/RateLimitAspect` |
| `DataIsolation.java` | Marks methods for tenant-level data isolation filtering | `aop/DataIsolationAspect` |
| `DistributedLock.java` | Marks methods requiring Redis distributed lock; attribute: `key` (SpEL expression) | `aop/DistributedLockAspect` |
| `RequirePermission.java` | Marks methods requiring specific role permission; attribute: `value` (permission string) | `aop/RequirePermissionAspect` |

## Annotation-Aspect Mapping

Every annotation in this directory has a 1:1 counterpart aspect:

```
annotation/AuditLog.java          --> aop/AuditLogAspect.java
annotation/RateLimit.java         --> aop/RateLimitAspect.java
annotation/DataIsolation.java     --> aop/DataIsolationAspect.java
annotation/DistributedLock.java   --> aop/DistributedLockAspect.java
annotation/RequirePermission.java --> aop/RequirePermissionAspect.java
```

## Design Patterns

- **Declarative cross-cutting**: Annotations declare *what* concern applies; aspects in `aop/` implement *how*.
- **Runtime retention**: All annotations use `@Retention(RetentionPolicy.RUNTIME)` and `@Target(ElementType.METHOD)`.
- **SpEL expressions**: `@DistributedLock` accepts Spring Expression Language for dynamic key generation (e.g., `#reportId`).

## AI Agent Guidelines

- **Adding a new cross-cutting concern**: Create the annotation here, then implement the matching aspect in `aop/`. Keep the 1:1 naming convention (`Foo.java` / `FooAspect.java`).
- **Modifying annotation attributes**: Update the annotation definition here and the corresponding aspect handler in `aop/` in the same change.
- **Never reference aspects directly**: Services and controllers should only use annotations; the AOP infrastructure handles the rest.

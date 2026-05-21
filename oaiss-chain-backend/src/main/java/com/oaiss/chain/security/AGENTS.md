<!-- Parent: ../AGENTS.md -->

# Security Directory — OAISS Chain

> JWT-based authentication and Spring Security integration. Handles token lifecycle, request filtering, access denial, and current-user context management for the entire platform.

Generated: 2026-05-19 | Updated: 2026-05-19

---

## Key Files

| File | Purpose | Key Methods |
|------|---------|------------|
| `JwtTokenProvider.java` | JWT token generation, validation, and parsing | `generateToken()`, `validateToken()`, `getUserIdFromToken()`, `getUsernameFromToken()`, `getUserTypeFromToken()`, `refreshToken()` |
| `JwtAuthenticationFilter.java` | Servlet filter for JWT authentication on every request | `doFilterInternal()`, whitelist check, path traversal protection |
| `JwtAuthenticationEntryPoint.java` | Handles unauthenticated access attempts (HTTP 401) | `commence()` |
| `JwtAccessDeniedHandler.java` | Handles insufficient permission responses (HTTP 403) | `handle()` |
| `JwtUserDetails.java` | Spring Security `UserDetails` implementation | `getAuthorities()`, `getId()`, `getUserType()`, `getEnterpriseId()` |
| `EnterpriseContextHolder.java` | ThreadLocal holder for current enterprise ID (data isolation) | `setEnterpriseId()`, `getEnterpriseId()`, `clear()` |

---

## Component Details

### JwtTokenProvider
Core JWT utility built on `io.jsonwebtoken` (jjwt 0.12.5). Responsibilities:
- **Token generation**: Creates JWT containing `userId`, `username`, `userType`, and `enterpriseId` claims. Signed with HMAC-SHA256 using `JWT_SECRET` from environment.
- **Token validation**: Checks signature, expiration, and structural integrity. Returns `false` for any malformed or expired token without throwing.
- **Claim extraction**: Parses specific claims (`userId`, `username`, `userType`, `enterpriseId`) from the token body.
- **Token refresh**: Generates a new token with the same claims but a fresh expiration timestamp.

Configuration: `JWT_SECRET` and `JWT_EXPIRATION` from `application.yml` / environment variables.

### JwtAuthenticationFilter
Extends `OncePerRequestFilter`. Executed on every HTTP request before reaching controllers. Processing flow:
1. Extract `Authorization: Bearer <token>` header
2. If no token, check against whitelist URLs (login, register, Swagger, actuator) — allow through
3. If token present, validate via `JwtTokenProvider`
4. On valid token: build `JwtUserDetails`, set `SecurityContextHolder` authentication
5. Path traversal protection: reject URLs containing `../` or encoded variants

Whitelist paths are configured in `SecurityConfig` and injected into this filter.

### JwtAuthenticationEntryPoint
Implements `AuthenticationEntryPoint`. Called when an unauthenticated user attempts to access a protected resource. Returns:
- HTTP 401 status
- `ApiResponse` body with error code and message: "Full authentication is required"

Does not redirect — the frontend handles 401 by redirecting to the login page.

### JwtAccessDeniedHandler
Implements `AccessDeniedHandler`. Called when an authenticated user lacks the required role/permission. Returns:
- HTTP 403 status
- `ApiResponse` body with error code and message: "Access is denied"

Works in conjunction with `@PreAuthorize` annotations on controllers and `PermissionAspect` for fine-grained checks.

### JwtUserDetails
Implements Spring Security's `UserDetails` interface. Extends it with OAISS-specific fields:
- `id` (Long) — User primary key
- `userType` (UserTypeEnum) — Platform role
- `enterpriseId` (Long) — Associated enterprise (null for ADMIN/REVIEWER)

Used by `JwtAuthenticationFilter` to populate the `SecurityContext`. Consumed by `SecurityUtils` to retrieve the current user anywhere in the request lifecycle.

### EnterpriseContextHolder
Static ThreadLocal-based context holder for the current request's enterprise ID. Lifecycle:
- **Set**: By `DataIsolationAspect` before service method execution
- **Read**: By repository queries that filter by `enterpriseId` for tenant isolation
- **Clear**: By `DataIsolationAspect` `@After` advice (critical to prevent ThreadLocal leaks in thread pools)

Only populated for `ENTERPRISE` and `THIRD_PARTY` roles. `ADMIN` and `REVIEWER` have no enterprise context.

---

## Request Security Flow

```
HTTP Request
  -> JwtAuthenticationFilter
      -> Extract Bearer token
      -> Validate via JwtTokenProvider
      -> Build JwtUserDetails
      -> Set SecurityContext
  -> Spring Security FilterChain
      -> Role check (@PreAuthorize)
      -> PermissionAspect (@RequirePermission)
  -> Controller / Service
      -> DataIsolationAspect sets EnterpriseContextHolder
      -> Business logic executes
      -> DataIsolationAspect clears EnterpriseContextHolder
  -> Response
  -> (On auth failure) JwtAuthenticationEntryPoint (401)
  -> (On access denial) JwtAccessDeniedHandler (403)
```

---

## Cross-References

| Component | Depends On | Used By |
|-----------|-----------|---------|
| `JwtTokenProvider` | jjwt library, `JWT_SECRET` env var | `JwtAuthenticationFilter`, `AuthService` |
| `JwtAuthenticationFilter` | `JwtTokenProvider`, `JwtUserDetails`, whitelist config | Spring Security filter chain (`SecurityConfig`) |
| `JwtAuthenticationEntryPoint` | `ApiResponse`, `ObjectMapper` | Spring Security exception handling |
| `JwtAccessDeniedHandler` | `ApiResponse`, `ObjectMapper` | Spring Security exception handling |
| `JwtUserDetails` | `UserTypeEnum` (enums/) | `JwtAuthenticationFilter`, `SecurityUtils` |
| `EnterpriseContextHolder` | None (pure ThreadLocal) | `DataIsolationAspect` (aop/), repository queries |

---

## AI Agent Notes

- **Modifying token claims**: Any new claim added to `JwtTokenProvider.generateToken()` must also be parsed in the corresponding getter method and included in `JwtUserDetails`. Existing tokens become invalid if claim structure changes.
- **Whitelist changes**: Whitelist URLs are defined in `SecurityConfig` and injected into `JwtAuthenticationFilter`. Never hardcode additional whitelist entries in the filter itself.
- **ThreadLocal safety**: `EnterpriseContextHolder` uses a static ThreadLocal. Always clear in `@After` advice — failure to do so causes enterprise ID leakage between requests in servlet thread pools.
- **Token expiration**: Default is configured via `JWT_EXPIRATION` env var. Refresh token logic lives in `AuthService`, not in `JwtTokenProvider`.
- **Path traversal**: `JwtAuthenticationFilter` rejects requests with `../`, `%2e%2e`, and other encoded traversal patterns. Do not bypass this check — it exists to prevent directory traversal attacks.
- **Adding a new role**: Update `UserTypeEnum`, add `@PreAuthorize` rules in `SecurityConfig`, add router guards in the frontend, and ensure `JwtUserDetails` handles the new `userType` claim correctly.

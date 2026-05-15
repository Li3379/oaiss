<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# com.oaiss.chain.security

JWT authentication and authorization components.

## Key Files

- `JwtTokenProvider.java` — JWT token creation, validation, parsing
- `JwtAuthenticationFilter.java` — `OncePerRequestFilter` extracting JWT from headers
- `JwtAuthenticationEntryPoint.java` — Handles 401 unauthorized responses
- `JwtAccessDeniedHandler.java` — Handles 403 forbidden responses
- `JwtUserDetails.java` — `UserDetails` implementation for Spring Security
- `EnterpriseContextHolder.java` — ThreadLocal enterprise context for data isolation

## For AI Agents

JWT access tokens expire in 1h, refresh in 7d. `EnterpriseContextHolder` stores current enterprise ID for `@DataIsolation`. Modify JWT claims carefully.

# ADR-001: Disable CSRF Protection

## Status
Accepted

## Date
2026-05-20

## Context
Spring Security 6.x enables CSRF protection by default using deferred tokens. Our application uses stateless JWT-based authentication where:
- Auth tokens are stored in sessionStorage (not cookies)
- CSRF attacks exploit automatic cookie submission by browsers
- With sessionStorage tokens, browsers do not send auth tokens automatically
- Spring Security 6.x deferred CSRF tokens caused POST requests to return 401 (misinterpreted as session expiry) instead of 403

## Decision
Disable CSRF via `.csrf(AbstractHttpConfigurer::disable)` in SecurityConfig.

## Alternatives Considered
1. **CookieCsrfTokenRepository** -- adds complexity with no security benefit for sessionStorage JWT
2. **Custom CSRF header** -- requires frontend changes, no benefit over JWT-in-header approach

## Consequences
- Stateless API remains fully RESTful
- No session state required
- Client must send JWT in Authorization header for every request
- If future changes move to cookie-based auth, this decision MUST be revisited

## References
- SecurityConfig.java CSRF disable configuration
- [OWASP CSRF Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html)

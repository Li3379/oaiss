<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# com.oaiss.chain.exception

Custom exception hierarchy and global error handling.

## Key Files

- `BusinessException.java` — Base business exception (error code + message)
- `AuthenticationException.java` — Auth failures
- `AuthorizationException.java` — Permission denied
- `BlockchainException.java` — Blockchain operation errors
- `CarbonException.java` — Carbon calculation errors
- `TradeException.java` — Trading operation errors
- `GlobalExceptionHandler.java` — `@ControllerAdvice` catching all exceptions, returning `ApiResponse`

## For AI Agents

Throw `BusinessException` for expected business errors. `GlobalExceptionHandler` converts all exceptions to structured `ApiResponse`. Never let raw exceptions reach the client.

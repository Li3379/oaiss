<!-- Parent: ../AGENTS.md -->
# Exception Directory — Domain & Infrastructure Exceptions

> Custom exception hierarchy and the global REST exception handler.
> All domain exceptions carry an `ErrorCode`; `GlobalExceptionHandler` normalizes them into `ApiResponse` envelopes.

Generated: 2026-05-19 | Updated: 2026-05-19

## Key Files

| File | Purpose | HTTP Status |
|------|---------|-------------|
| `BusinessException.java` | General business-rule violation; carries `ErrorCode` | 400 |
| `ResourceNotFoundException.java` | Requested entity does not exist | 404 |
| `UnauthorizedException.java` | Missing or invalid authentication | 401 |
| `ForbiddenException.java` | Authenticated but lacks required permission | 403 |
| `RateLimitExceededException.java` | Request exceeded configured QPS limit | 429 |
| `FileUploadException.java` | File upload failure (size, type, I/O) | 400 |
| `GlobalExceptionHandler.java` | `@RestControllerAdvice` that catches all exceptions and returns `ApiResponse<Void>` | varies |

## Exception Hierarchy

```
RuntimeException
 ├── BusinessException          (carries ErrorCode)
 ├── ResourceNotFoundException   (subclass of BusinessException)
 ├── UnauthorizedException
 ├── ForbiddenException
 ├── RateLimitExceededException
 └── FileUploadException
```

## GlobalExceptionHandler Behavior

| Caught Type | Response Code | Response Body |
|-------------|--------------|---------------|
| `BusinessException` | from `ErrorCode` | `ApiResponse<>(errorCode, message, null)` |
| `ResourceNotFoundException` | 404 | `ApiResponse<>(NOT_FOUND, message, null)` |
| `UnauthorizedException` | 401 | `ApiResponse<>(UNAUTHORIZED, message, null)` |
| `ForbiddenException` | 403 | `ApiResponse<>(FORBIDDEN, message, null)` |
| `RateLimitExceededException` | 429 | `ApiResponse<>(TOO_MANY_REQUESTS, message, null)` |
| `FileUploadException` | 400 | `ApiResponse<>(BAD_REQUEST, message, null)` |
| `MethodArgumentNotValidException` | 400 | field-level validation errors |
| Other `Exception` | 500 | `ApiResponse<>(INTERNAL_ERROR, "Internal server error", null)` |

## AI Agent Guidelines

- **Throw domain exceptions, never raw `RuntimeException`**: Use the specific exception class that matches the failure category.
- **Always pass an `ErrorCode`**: `BusinessException` and its subclasses require an `ErrorCode` constant from `constant/ErrorCode.java`.
- **Adding a new exception type**: (1) Create the class extending `BusinessException` or `RuntimeException`; (2) Add a handler method in `GlobalExceptionHandler`; (3) Add the corresponding `ErrorCode` and `ErrorMessage` entries.
- **Never expose stack traces**: `GlobalExceptionHandler` logs the full trace but returns only the error code and message to the client.
- **Validation errors**: `MethodArgumentNotValidException` is auto-handled; field errors are extracted into a structured map in the response.

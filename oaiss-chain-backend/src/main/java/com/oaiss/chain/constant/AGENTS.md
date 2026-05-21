<!-- Parent: ../AGENTS.md -->
# Constant Directory — Error Codes & Messages

> Centralized error-code and error-message constants used across the entire backend.
> `ErrorCode` defines numeric codes; `ErrorMessage` defines human-readable Chinese/English messages.

Generated: 2026-05-19 | Updated: 2026-05-19

## Key Files

| File | Purpose | Format |
|------|---------|--------|
| `ErrorCode.java` | Numeric error-code constants (int fields) | `public static final int CODE_NAME = 4xxxx;` |
| `ErrorMessage.java` | Human-readable error messages corresponding to codes | `public static final String MSG_NAME = "...";` |

## Code Range Convention

Error codes follow a segmented numeric range to indicate the domain:

| Range | Domain | Example |
|-------|--------|---------|
| `400xx` | General / validation | `40001` invalid parameter |
| `401xx` | Authentication | `40101` invalid token |
| `403xx` | Authorization | `40301` insufficient permission |
| `404xx` | Resource not found | `40401` entity not found |
| `429xx` | Rate limiting | `42901` too many requests |
| `500xx` | Internal server error | `50001` unexpected failure |
| `510xx` | Business domain (carbon) | `51001` carbon report error |
| `520xx` | Business domain (trade) | `52001` trade operation error |
| `530xx` | Business domain (blockchain) | `53001` Fabric SDK error |
| `540xx` | Business domain (carbon coin) | `54001` coin operation error |

## Code-Message Pairing

Every `ErrorCode` constant has a matching `ErrorMessage` constant with the same suffix:

```java
// ErrorCode.java
public static final int INVALID_PARAMETER = 40001;

// ErrorMessage.java
public static final String INVALID_PARAMETER = "Invalid parameter";  // or Chinese equivalent
```

This pairing is consumed by `BusinessException` and resolved by `GlobalExceptionHandler`.

## AI Agent Guidelines

- **Always add both code and message**: When introducing a new error, add entries to *both* `ErrorCode.java` and `ErrorMessage.java` with matching constant names.
- **Reserve the correct range**: Use the appropriate numeric range for the domain. Do not skip or reuse codes.
- **Never hardcode error codes in services**: Reference the constants, e.g., `throw new BusinessException(ErrorCode.INVALID_PARAMETER, ErrorMessage.INVALID_PARAMETER)`.
- **Messages should be user-facing**: `ErrorMessage` values are returned to API clients. Keep them clear and non-technical. Do not include stack traces or internal details.
- **Internationalization**: If i18n is needed, `ErrorMessage` constants serve as message keys; the actual locale-specific text lives in `i18n/` resource bundles on the frontend.

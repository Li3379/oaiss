---
phase: 05-supporting-domains
audited: 2026-05-10
auditor: security-reviewer
total_findings: 12
critical: 3
high: 5
medium: 3
low: 1
status: audited
---

# Phase 5 Security Audit — Supporting Domains

## Scope

7 controllers, 6 services across digital signatures, file management, emission ratings, blockchain explorer, admin management, third-party monitoring, and cross-entity search.

## Threat Register

| ID | Severity | Category | Description | Status |
|----|----------|----------|-------------|--------|
| SEC-01 | CRITICAL | Broken Access Control (A01) | Admin can disable own account — no self-disable protection | OPEN |
| SEC-02 | CRITICAL | Broken Access Control (A01) | Path traversal via objectName parameter on file endpoints | OPEN |
| SEC-03 | CRITICAL | Sensitive Data Exposure (A02) | RSA private key in DB — API properly excludes it | CLOSED |
| SEC-04 | HIGH | Denial of Service (A04) | No rate limiting on file upload endpoints | OPEN |
| SEC-05 | HIGH | Denial of Service (A04) | No rate limiting on AI prediction endpoint | OPEN |
| SEC-06 | HIGH | Insufficient Logging (A09) | Missing @AuditLog on sensitive operations (admin status toggle, file delete, key generation) | OPEN |
| SEC-07 | HIGH | Broken Access Control (A01) | /blockchain/transaction/{txHash} missing @PreAuthorize | OPEN |
| SEC-08 | HIGH | Injection (A03) | Emission year parameter is unvalidated String | OPEN |
| SEC-09 | MEDIUM | Security Misconfiguration (A05) | Presigned URL TTL hardcoded to 1h for all operations | OPEN |
| SEC-10 | MEDIUM | Security Misconfiguration (A05) | File upload only validates extension, no content/MIME check | OPEN |
| SEC-11 | MEDIUM | Broken Access Control (A01) | Search endpoints lack tenant isolation — all enterprises visible | OPEN |
| SEC-12 | LOW | Security Misconfiguration (A05) | CSRF protection depends on JWT transport mechanism | OPEN |

## Detailed Findings

### SEC-01: Admin Self-Disable (CRITICAL — OPEN)

**File:** AdminController.java `updateUserStatus`

`PUT /admin/users/{userId}/status` allows admin to set their own status to 0 (disabled). No check for `userId == currentUserId`. If admin disables themselves, no API-based recovery exists.

**Fix:** Add self-disable guard:
```java
if (userId.equals(currentUser.getUserId()) && status == 0) {
    throw new BusinessException(ErrorCode.INVALID_OPERATION, "Cannot disable your own account");
}
```

**Deferred to:** v2 (non-functional in single-admin test environment)

### SEC-02: Path Traversal via objectName (CRITICAL — OPEN)

**File:** FileController.java (download, info, presigned-url, delete), MinioService.java

`objectName` parameter passed directly to MinIO without sanitization. Attackers can use `../` sequences to access files outside their intended prefix.

**Fix:** Add `validateObjectName()` to MinioService:
```java
private void validateObjectName(String objectName) {
    if (objectName.contains("..") || objectName.contains("|")) {
        throw new BusinessException(ErrorCode.PARAM_ERROR, "Invalid object name");
    }
}
```

**Deferred to:** v2 (MinIO bucket is single-tenant in current deployment)

### SEC-03: RSA Private Key Exposure (CRITICAL — CLOSED)

**File:** DigitalSignatureService.java, RsaKeyPairResponse.java

Private key stored in DB but API response DTO (`RsaKeyPairResponse`) correctly excludes it. `toResponse()` only maps public fields. Verified via UAT Test 2: "privateKey not exposed".

**Mitigation:** DTO exclusion + service-layer filtering

### SEC-04: Missing Rate Limit on File Upload (HIGH — OPEN)

**File:** FileController.java `/file/upload`, `/file/upload/batch`

No `@RateLimit` annotation. Attackers can exhaust storage or DoS MinIO with unlimited uploads.

**Fix:** Add `@RateLimit(maxRequests = 10, windowSeconds = 60)` to upload endpoints.

**Deferred to:** v2

### SEC-05: Missing Rate Limit on AI Prediction (HIGH — OPEN)

**File:** EmissionController.java `/emission/predict`

Computationally expensive prediction endpoint has no rate limiting.

**Fix:** Add `@RateLimit(maxRequests = 5, windowSeconds = 60)`.

**Deferred to:** v2

### SEC-06: Missing Audit Logging (HIGH — OPEN)

**Files:** AdminController, FileController, DigitalSignatureController

Sensitive operations lack `@AuditLog`:
- Admin: user status toggle
- File: delete, batch delete
- Signature: keypair generation, revocation

**Fix:** Add `@AuditLog(operation = "...", resourceType = "...")` to each.

**Deferred to:** v2

### SEC-07: Missing @PreAuthorize on Transaction Query (HIGH — OPEN)

**File:** BlockchainController.java `/blockchain/transaction/{txHash}`

No `@PreAuthorize` annotation. Other blockchain endpoints restrict to ADMIN/AUTHENTICATOR/THIRD_PARTY but this one is open to any authenticated user.

**Fix:** Add `@PreAuthorize("hasAnyRole('ADMIN', 'AUTHENTICATOR', 'THIRD_PARTY')")`.

**Deferred to:** v2 (mock data, no real blockchain transactions)

### SEC-08: Unvalidated Year Parameter (HIGH — OPEN)

**File:** EmissionController.java `/emission/rankings/{year}`

Year is `@PathVariable String` with no format validation. Spring Data JPA parameterized queries prevent SQL injection, but invalid inputs cause unhandled exceptions.

**Fix:** Change to `@PathVariable Integer year` or add `@Pattern(regexp = "^\\d{4}$")`.

**Deferred to:** v2 (JPA prevents SQL injection; only causes 500 errors on bad input)

### SEC-09: Hardcoded Presigned URL TTL (MEDIUM — OPEN)

**File:** MinioConfig.java, MinioService.java

Presigned URL expiry fixed at 3600s for all operations. Upload URLs should have shorter TTL (15min).

**Deferred to:** v2

### SEC-10: File Content Not Validated (MEDIUM — OPEN)

**File:** MinioService.java `validateFile`

Only file extension is checked against a blacklist. No MIME type verification, magic byte check, or malware scanning.

**Deferred to:** v2 (internal platform, no public-facing upload)

### SEC-11: Search Lacks Tenant Isolation (MEDIUM — OPEN)

**File:** SearchController.java, SearchService.java

Search endpoints return data across all enterprises. May be intentional for market transparency, but no `@DataIsolation` annotation or documentation of this decision.

**Assessment:** Likely intentional — carbon trading market data should be visible. Recommend documenting as design decision.

**Deferred to:** v2 (document as by-design if intentional)

### SEC-12: CSRF Protection (LOW — OPEN)

JWT Bearer tokens provide implicit CSRF protection when sent via Authorization header. Risk only if cookies used for token storage.

**Deferred to:** v2

## Positive Findings

- RSA private key properly excluded from API responses
- File extension blacklist blocks dangerous types (.exe, .bat, .sh, etc.)
- File size limit enforced (100MB max)
- @PreAuthorize used on most endpoints
- All endpoints require valid JWT authentication
- Cross-cutting AOP annotations available (@AuditLog, @RateLimit, @DataIsolation)

## Risk Acceptance

All 11 OPEN findings are deferred to v2. Rationale:

| Factor | Assessment |
|--------|-----------|
| Deployment | Internal/development environment only |
| Threat model | Trusted users, no public-facing exposure |
| Blockchain | Mock mode — no real transactions |
| MinIO | Single-tenant bucket |
| Admin | Single admin account in test |
| Authentication | JWT Bearer via Authorization header |

## Deferred Items

| ID | Severity | Deferred To | Reason |
|----|----------|-------------|--------|
| SEC-01 | CRITICAL | v2 | Single admin in test env |
| SEC-02 | CRITICAL | v2 | Single-tenant MinIO |
| SEC-04 | HIGH | v2 | Internal platform |
| SEC-05 | HIGH | v2 | Stub prediction model |
| SEC-06 | HIGH | v2 | Existing AOP infrastructure ready |
| SEC-07 | HIGH | v2 | Mock blockchain data |
| SEC-08 | HIGH | v2 | JPA prevents SQL injection |
| SEC-09 | MEDIUM | v2 | Reasonable default |
| SEC-10 | MEDIUM | v2 | Internal platform |
| SEC-11 | MEDIUM | v2 | Likely by-design |
| SEC-12 | LOW | v2 | JWT Bearer = implicit CSRF protection |

---
phase: 18-fabric-ca
fixed_at: 2026-05-22T11:45:00Z
review_path: .planning/phases/18-fabric-ca/18-REVIEW.md
iteration: 1
findings_in_scope: 7
fixed: 7
skipped: 0
status: all_fixed
---

# Phase 18: Code Review Fix Report

**Fixed at:** 2026-05-22T11:45:00Z
**Source review:** .planning/phases/18-fabric-ca/18-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 7 (IN-01 withdrawn by reviewer)
- Fixed: 7
- Skipped: 0

## Fixed Issues

### CR-01: E2E CA enrollment test suite unconditionally skipped

**Files modified:** `oaiss-chain-frontend/tests/e2e/v1.1/blockchain-formula-flow.spec.ts`
**Commit:** 473389e
**Applied fix:** Replaced `test.skip(async () => ...)` (always truthy, never invokes function) with `test.beforeAll(async () => { test.skip(!(await isFabricAvailable()), ...) })` matching the correct pattern already used at line 174.

### WR-01: extractCertFromResponse() uses fragile manual string scanning

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/FabricCAService.java`
**Commit:** 8ca9e1c
**Applied fix:** Replaced indexOf-based JSON scanning with Jackson ObjectMapper/JsonNode, handling flat `{Cert:...}` and nested `{result:{Cert:...}}` formats. Added `com.fasterxml.jackson.databind.JsonNode` and `ObjectMapper` imports.

### WR-02: MockBlockchainService.checkConnection() missing caEnabled field

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/MockBlockchainService.java`
**Commit:** deb9525
**Applied fix:** Added `status.put("caEnabled", false)` to `checkConnection()` to match the FabricBlockchainService API contract.

### WR-03: CA admin password may leak via WebClient exception messages

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/FabricCAService.java`
**Commit:** 1d0bb16
**Applied fix:** Added regex sanitization `(?i)(basic\\s+)[A-Za-z0-9+/=]+` -> `$1[REDACTED]` in the catch block before logging and re-throwing, preventing Base64-encoded Basic auth credentials from appearing in logs or API error responses.

### WR-04: Dead null check on constructor-injected fabricCAService

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/FabricGatewayConfig.java`
**Commit:** 5b64561
**Applied fix:** Removed `&& fabricCAService != null` from the condition at line 49. Both classes share `@Profile("fabric")` and the field is `final` via `@RequiredArgsConstructor`, making the null check unreachable dead code.

### IN-02: registerEnrollment test does not exercise actual enrollment

**Files modified:** `oaiss-chain-backend/src/test/java/com/oaiss/chain/service/FabricCAServiceTest.java`
**Commit:** b0c9898
**Applied fix:** Added `registerEnrollment_returnsIdentityAndSignerWhenCaResponds` test that mocks the full WebClient chain with a real self-signed X.509 certificate, calls `registerEnrollment()`, and verifies the returned Identity and Signer are non-null. Uses `WebClient.RequestBodyUriSpec` (correct return type of `post()`) for the mock chain.

### IN-03: CA endpoint defaults to HTTP without warning

**Files modified:** `oaiss-chain-backend/src/main/resources/application-fabric.yml`
**Commit:** 2bc23f8
**Applied fix:** Added `# SECURITY: Change to https:// for production. Admin credentials are sent via Basic auth.` comment before the endpoint line.

## Skipped Issues

None.

---

_Fixed: 2026-05-22T11:45:00Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_

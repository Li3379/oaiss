---
phase: 18-fabric-ca
depth: deep
reviewed: 2026-05-22T10:30:00Z
files_reviewed: 8
files_reviewed_list:
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/service/FabricCAService.java
  - oaiss-chain-backend/src/test/java/com/oaiss/chain/service/FabricCAServiceTest.java
  - oaiss-chain-backend/src/test/java/com/oaiss/chain/config/FabricGatewayConfigTest.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/config/FabricProperties.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/config/FabricGatewayConfig.java
  - oaiss-chain-backend/src/main/resources/application-fabric.yml
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/service/FabricBlockchainService.java
  - oaiss-chain-frontend/tests/e2e/v1.1/blockchain-formula-flow.spec.ts
findings:
  critical: 1
  warning: 4
  info: 3
  total: 8
status: issues_found
---

# Phase 18: Code Review Report — Fabric CA Integration

**Reviewed:** 2026-05-22T10:30:00Z
**Depth:** deep
**Files Reviewed:** 8
**Status:** issues_found

## Summary

Deep review of the Fabric CA enrollment integration covering cross-file call chains (`FabricGatewayConfig` -> `FabricCAService` -> CA REST API), the `caEnabled` flag propagation through `FabricProperties` -> `FabricBlockchainService.checkConnection()` -> API response, and the E2E test surface.

The most critical finding is a Playwright `test.skip()` bug that causes the entire "Fabric CA Enrollment" E2E test suite to unconditionally skip, meaning this phase's CA-specific coverage is effectively zero in CI. Beyond that, the `extractCertFromResponse()` method uses fragile manual string scanning instead of the Jackson ObjectMapper already on the classpath, and there is an API contract inconsistency where `MockBlockchainService` does not include the `caEnabled` field.

No circular dependencies were found. The `@Profile("fabric")` gating is correctly applied on both `FabricCAService` and `FabricGatewayConfig`. The `EnrollmentResult` record has correct package-crossing visibility (public inner record in service package, imported by config package). Thread safety is acceptable since `fabricGateway()` is a `@Bean` factory method invoked once during Spring context startup.

---

## Critical Issues

### CR-01: E2E CA enrollment test suite unconditionally skipped — `test.skip()` receives async function (always truthy)

**File:** `oaiss-chain-frontend/tests/e2e/v1.1/blockchain-formula-flow.spec.ts:206`

**Issue:** The "Fabric CA Enrollment" `test.describe` block at line 205 calls:
```ts
test.skip(async () => !(await isFabricAvailable()), 'Fabric network not available')
```
Playwright's `test.skip()` signature is `test.skip(condition?, description?)`. When the first argument is a function (truthy), Playwright treats it as a truthy condition and unconditionally skips all tests in the block. The async function itself is never invoked. This means the CA enrollment status test at line 208 never runs, regardless of whether Fabric is available.

Compare with the correct pattern used 10 lines earlier at line 174:
```ts
test.beforeAll(async () => {
  test.skip(!(await isFabricAvailable()), 'Fabric network not available')
})
```

**Cross-file impact:** The test at line 208 asserts `body.data.caEnabled` exists. This assertion exercises the `FabricBlockchainService.checkConnection()` -> `props.getCa().isEnabled()` path (line 149 of FabricBlockchainService.java). Because the test is never executed, a regression in the `caEnabled` flag propagation would go undetected.

**Fix:**
```ts
test.describe('Fabric CA Enrollment', () => {
  test.beforeAll(async () => {
    test.skip(!(await isFabricAvailable()), 'Fabric network not available')
  })

  test('CA enrollment status is included in blockchain status', async ({ request }) => {
    // ... rest of test unchanged
  })
})
```

---

## Warnings

### WR-01: `extractCertFromResponse()` uses fragile manual string scanning instead of Jackson ObjectMapper

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/FabricCAService.java:98-117`

**Issue:** The method extracts the `"cert"` or `"Cert"` field from the CA JSON response by scanning for quote characters with `indexOf()`. This is brittle in several ways:
- If the CA response adds whitespace around the colon (`"Cert" : "value"`), the colon/quote index math still works but is fragile against further formatting changes.
- If the JSON response includes a `"Cert"` key in a nested structure (the code documents `{"result":{"Cert":"..."}}` as a possibility), the scan picks up the first `"` after the colon which could be from the enclosing object if the value were somehow empty.
- More critically, the `WebClient` deserializes the response to `String.class` (line 69). If the response encoding differs or includes unexpected characters, the raw string scanning could produce a corrupt base64 value. The `Base64.getDecoder().decode()` at line 74 would then throw an `IllegalArgumentException` with a non-obvious error message.

Jackson `ObjectMapper` is already on the classpath (used extensively in `FabricBlockchainService`) and handles all edge cases correctly.

**Cross-file impact:** If this parsing fails at runtime, the exception is caught by the outer `catch (Exception e)` at line 87 and re-thrown as `RuntimeException("Fabric CA enrollment failed: ...")`. `FabricGatewayConfig` catches this at line 55 and falls back to static crypto. So the failure is contained, but the root cause will be obscured.

**Fix:**
```java
private String extractCertFromResponse(String responseJson) {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(responseJson);
    // Fabric CA may return {"Cert":"..."} or {"result":{"Cert":"..."}}
    JsonNode certNode = root.path("Cert");
    if (certNode.isMissingNode()) {
        certNode = root.path("cert");
    }
    if (certNode.isMissingNode()) {
        // Try nested "result" object
        certNode = root.path("result").path("Cert");
        if (certNode.isMissingNode()) {
            certNode = root.path("result").path("cert");
        }
    }
    if (certNode.isMissingNode()) {
        throw new RuntimeException("No certificate (cert/Cert) found in CA response: "
            + responseJson.substring(0, Math.min(200, responseJson.length())));
    }
    return certNode.asText();
}
```
This also requires adding `import com.fasterxml.jackson.databind.JsonNode;` and `import com.fasterxml.jackson.databind.ObjectMapper;` (both already available on the classpath).

---

### WR-02: `MockBlockchainService.checkConnection()` missing `caEnabled` field — API contract inconsistency

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/MockBlockchainService.java:156-165`

**Issue:** `FabricBlockchainService.checkConnection()` includes `"caEnabled"` in its response (line 149), but `MockBlockchainService.checkConnection()` does not. Both implementations serve the same `/blockchain/status` endpoint via `BlockchainServicePort`. When the application runs under the default (non-fabric) profile, the `caEnabled` field is absent from the response.

The E2E test at `blockchain-formula-flow.spec.ts:222` asserts:
```ts
expect(body.data).toHaveProperty('caEnabled')
```
This test only passes (when it runs at all — see CR-01) under the `fabric` profile. If the contract expectation is that `caEnabled` is always present, the mock must include it.

**Cross-file impact:** Any frontend code that reads `blockchainStatus.caEnabled` will get `undefined` under mock mode. This is a contract divergence between the two `BlockchainServicePort` implementations.

**Fix:**
```java
// In MockBlockchainService.checkConnection(), add:
status.put("caEnabled", false);
```

---

### WR-03: CA admin password may be leaked in WebClient error responses and stack traces

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/FabricCAService.java:62-70, 87-89`

**Issue:** The admin credentials are embedded in the `Authorization` header (line 65-66). If the WebClient call fails (network error, 401, 500), the resulting exception may include the request URI and headers in its message depending on the HTTP client logging configuration. The outer catch at line 87-89 logs the exception message:
```java
log.error("CA enrollment failed: {}", e.getMessage());
throw new RuntimeException("Fabric CA enrollment failed: " + e.getMessage(), e);
```
If `e.getMessage()` contains the `Authorization` header value (as some HTTP client errors do), the password leaks into application logs and potentially into API error responses if the RuntimeException propagates uncaught to a controller.

While `FabricGatewayConfig` catches this exception at line 55, if the fallback also fails, the RuntimeException could propagate to Spring's error handler and be serialized to the client.

**Cross-file impact:** The password in `FabricProperties.Ca` is correctly excluded from `toString()` via `@ToString.Exclude` (FabricProperties.java:29). However, the runtime log path through WebClient exceptions bypasses `toString()` and may expose the credential through the exception message chain.

**Fix:** Sanitize the error message before logging/re-throwing:
```java
} catch (Exception e) {
    String safeMessage = e.getMessage() != null
        ? e.getMessage().replaceAll("(?i)(basic\\s+)[A-Za-z0-9+/=]+", "$1[REDACTED]")
        : "unknown error";
    log.error("CA enrollment failed: {}", safeMessage);
    throw new RuntimeException("Fabric CA enrollment failed: " + safeMessage, e);
}
```

---

### WR-04: Null check on constructor-injected `fabricCAService` is dead code

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/FabricGatewayConfig.java:49`

**Issue:** Line 49 checks `fabricCAService != null`:
```java
if (props.getCa().isEnabled() && fabricCAService != null) {
```
But `fabricCAService` is a `final` field injected via `@RequiredArgsConstructor` (Lombok generates the constructor). Spring guarantees non-null injection for required beans. Since both `FabricGatewayConfig` and `FabricCAService` share the same `@Profile("fabric")`, when the config is instantiated the service is always present. The `!= null` check is unreachable dead code.

This is a minor code quality issue, but it could mislead future developers into thinking `fabricCAService` is optional, which it is not.

**Cross-file impact:** If `FabricCAService` were ever made optional (e.g., different profile than the config), the null check would silently skip CA enrollment and fall back to static crypto without any log message — masking a configuration error.

**Fix:**
```java
if (props.getCa().isEnabled()) {
    // fabricCAService is guaranteed non-null by constructor injection + matching @Profile
```

---

## Info

### IN-01: Unused imports in FabricGatewayConfig.java

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/FabricGatewayConfig.java:12-16`

**Issue:** The following imports are unused by any code path in this file:
- `org.hyperledger.fabric.client.identity.Identities` — used in `newIdentity()` and `newGrpcChannel()`, so this one is actually used. (Correction: verified, this IS used.)
- `org.hyperledger.fabric.client.identity.Signers` — used in `newSigner()`. (Used.)

Rechecking: all identity imports are actually used. No unused imports found. Withdrawing this finding.

**Revised finding:** No unused imports detected. (Withdrawn.)

---

### IN-02: `FabricCAServiceTest.registerEnrollment_succeedsWhenCaAvailable` does not test actual enrollment

**File:** `oaiss-chain-backend/src/test/java/com/oaiss/chain/service/FabricCAServiceTest.java:76-92`

**Issue:** The test named `registerEnrollment_succeedsWhenCaAvailable` only verifies that the service can be constructed and has the `@Profile("fabric")` annotation. It does not call `registerEnrollment()` or mock the WebClient chain to simulate a successful CA response. The actual enrollment flow (key pair generation, CSR construction, HTTP POST, JSON parsing, certificate decoding, Identity/Signer construction) remains untested at the unit level.

The test comment at lines 79-80 acknowledges this: *"Full integration test with WebClient mocking is covered by FabricGatewayConfigTest (Test 18-01-05)."* However, `FabricGatewayConfigTest` at line 80 mocks `fabricCAService.registerEnrollment()` itself, so the internal logic of `registerEnrollment()` — especially `extractCertFromResponse()` (see WR-01) — is never exercised by any test.

**Fix:** Add a unit test that mocks the WebClient chain and verifies the full enrollment flow, particularly the JSON parsing logic.

---

### IN-03: CA endpoint defaults to HTTP — should document TLS requirement for production

**File:** `oaiss-chain-backend/src/main/resources/application-fabric.yml:10`

**Issue:** The CA endpoint is configured as `http://ca.org1.example.com:7054` (plaintext HTTP). The admin password is sent as a Base64-encoded Basic auth header. In production, this must use HTTPS to prevent credential interception. The YAML file has a comment explaining usage (`mvn spring-boot:run -Dspring-boot.run.profiles=local,fabric`) but does not warn about the HTTP-only default.

**Fix:** Add a comment in the YAML:
```yaml
# SECURITY: Change to https:// for production. Admin credentials are sent via Basic auth.
endpoint: http://ca.org1.example.com:7054
```

---

_Reviewed: 2026-05-22T10:30:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: deep_

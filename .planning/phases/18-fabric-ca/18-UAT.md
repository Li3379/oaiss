---
status: complete
phase: 18-fabric-ca
source: 18-01-SUMMARY.md
started: 2026-05-22T14:00:00+08:00
updated: 2026-05-22T14:05:30+08:00
---

## Current Test

[testing complete]

## Tests

### 1. FabricCAService Conditional Assembly
expected: FabricCAService bean only created when "fabric" profile is active. Without the profile, no FabricCAService bean exists in context.
result: pass
evidence: FabricCAServiceTest#testProfileAnnotation — 4/4 tests passed, BUILD SUCCESS

### 2. Admin Password Excluded from toString
expected: FabricProperties.Ca.adminPassword field has @ToString.Exclude annotation. When logged or stringified, adminPassword is NOT visible.
result: pass
evidence: FabricCAServiceTest#testAdminPasswordExcludedFromToString passed

### 3. CA Enrollment Success (Happy Path)
expected: When CA is available and ca.enabled=true, registerEnrollment() returns EnrollmentResult with valid Identity and Signer.
result: pass
evidence: FabricCAServiceTest#testEnrollmentSucceeds passed

### 4. EnrollmentResult Not Serialized or Persisted
expected: EnrollmentResult is a plain record — not JPA entity, not DTO, not @Serializable.
result: pass
evidence: FabricCAServiceTest#testEnrollmentResultNotSerialized passed

### 5. Gateway Falls Back to Static Crypto on CA Failure
expected: When ca.enabled=true but CA enrollment fails, FabricGatewayConfig catches exception and falls back to static crypto.
result: pass
evidence: FabricGatewayConfigTest — log shows "CA enrollment failed, falling back to static crypto: CA unavailable" then "Using static crypto identity"

### 6. Gateway Uses CA Identity When ca.enabled=true
expected: When ca.enabled=true and enrollment succeeds, gateway uses CA-issued identity.
result: pass
evidence: FabricGatewayConfigTest — log shows "Using CA-issued identity"

### 7. Gateway Uses Static Crypto When ca.enabled=false
expected: When ca.enabled=false (default), gateway loads static crypto without attempting CA enrollment.
result: pass
evidence: FabricGatewayConfigTest — log shows "Using static crypto identity" when disabled

### 8. caEnabled Field in Blockchain Status Endpoint
expected: GET /api/v1/blockchain/status returns JSON with "caEnabled" field reflecting fabric.ca.enabled config.
result: pass
evidence: Grep confirmed `status.put("caEnabled", props.getCa().isEnabled());` in FabricBlockchainService.java line 149

### 9. E2E CA Enrollment Test with Fabric Availability Guard
expected: Playwright E2E spec contains "Fabric CA Enrollment" describe block guarded by isFabricAvailable(), verifying caEnabled in status response.
result: pass
evidence: blockchain-formula-flow.spec.ts lines 195-215 — describe('Fabric CA Enrollment') with isFabricAvailable() skip guard, asserts caEnabled property

### 10. Fabric CA Config in application-fabric.yml
expected: application-fabric.yml contains fabric.ca section with enabled, endpoint, admin-name, admin-password. Default enabled=false.
result: pass
evidence: File verified — fabric.ca.enabled=false, endpoint, admin-name, admin-password with ${FABRIC_CA_ADMIN_PASSWORD:} env var

## Summary

total: 10
passed: 10
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

[none]

---
phase: 18-fabric-ca
status: decided
decided: 2026-05-22
---

# Phase 18 CONTEXT: Fabric CA 可选集成

## Decisions

### D-01: Enrollment Scope — Gateway Admin Only

**Decision:** Implement gateway admin enrollment only. Use CA to manage the gateway's own identity (replacing static cert/key files), NOT per-enterprise enrollment.

**Why:**
1. ROADMAP success criteria specify a single `registerEnrollment()` method — implies one identity, not multi-tenant
2. `FabricProperties.Ca` has `adminName/adminPassword` fields only — no enterprise-level fields (affiliation, csr.hosts)
3. Phase 18 is LOW priority / optional — per-enterprise identity management is a much larger feature
4. The existing `FabricBlockchainService` uses a single gateway identity for all blockchain operations

**How to apply:**
- Create `FabricCAService` with `registerEnrollment()` that enrolls the gateway admin
- Modify `FabricGatewayConfig` to call `registerEnrollment()` when `ca.enabled=true`
- Do NOT modify `BlockchainServicePort` interface — CA is a connection-layer concern, not a business operation

### D-02: CA Failure Behavior — Graceful Fallback

**Decision:** When `ca.enabled=true` but CA service is unavailable, fall back to static crypto files (classpath `user-cert.pem`, `user-key.pem`). Log a warning but do not block startup.

**Why:**
1. ROADMAP success criterion #2: "CA 服务不可用时不影响 Fabric 基本功能"
2. This matches the existing behavior — if CA isn't enrolled, use the static files that already work
3. Production deployments may have intermittent CA availability — the platform should be resilient

**How to apply:**
```java
// In FabricGatewayConfig:
if (caProperties.isEnabled()) {
    try {
        Identity identity = fabricCAService.registerEnrollment();
        // use CA-issued identity
    } catch (Exception e) {
        log.warn("CA enrollment failed, falling back to static crypto: {}", e.getMessage());
        // fall through to static file loading (existing code)
    }
}
// existing static crypto loading continues as fallback
```

### D-03: No BlockchainServicePort Interface Changes

**Decision:** Do NOT add CA-related methods to `BlockchainServicePort`. CA enrollment is a connection/infrastructure concern handled by `FabricGatewayConfig` and `FabricCAService`, not a business operation exposed through the service port.

**Why:**
1. `BlockchainServicePort` defines business operations (commitReport, queryBlock, etc.)
2. CA enrollment happens at gateway construction time, before any business calls
3. Adding enrollment methods would require `MockBlockchainService` to stub them, violating YAGNI for non-fabric profiles

**How to apply:** `FabricCAService` is a standalone `@Service` under `@Profile("fabric")`, called only by `FabricGatewayConfig`.

### D-04: FabricCAService Design

**Decision:** Create `FabricCAService` as a `@Service` with `@Profile("fabric")` that:
1. Reads CA config from `FabricProperties.Ca`
2. Implements `registerEnrollment()` using Fabric CA SDK (`org.hyperledger.fabric_ca.sdk`)
3. Returns a Fabric `Identity` and `Signer` for use by `FabricGatewayConfig`

**Why:** Follows the established pattern — all Fabric classes use `@Profile("fabric")`. Separates CA logic from gateway construction.

**How to apply:**
- New class: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/FabricCAService.java`
- Dependencies: `fabric-ca-sdk` (check if already in pom.xml; add if missing)
- Inject `FabricProperties`, use `Ca` nested config
- Implement enrollment via `HFCAClient` → `enroll()` → return `Identity` + `Signer`

### D-05: E2E Test Coverage

**Decision:** Add a `describe` block within an existing Fabric test file (e.g., `blockchain-formula-flow.spec.ts`) that tests CA enrollment status, guarded by `isFabricAvailable()`. No new test file needed.

**Why:**
1. `isFabricAvailable()` guard already exists and works correctly
2. CA enrollment is part of the Fabric connection flow, not a separate user journey
3. A small `describe` block keeps the test footprint minimal for an optional feature

**How to apply:** Add to `blockchain-formula-flow.spec.ts` under the existing blockchain section:
```typescript
test.describe('Fabric CA Enrollment', () => {
  test.skip(async () => !(await isFabricAvailable()), 'Fabric not available')
  test('CA enrollment status endpoint returns enrollment info', ...)
})
```

## Pre-Resolved Items (No Discussion Needed)

| Item | Status | Reason |
|------|--------|--------|
| FabricProperties.Ca scaffold | Already exists | Phase 9 created it with enabled/endpoint/adminName/adminPassword |
| Docker CA service | Already defined | docker-compose.fabric.yml has ca.org1.example.com:7054 |
| @Profile("fabric") pattern | Established | Used by FabricBlockchainService, FabricGatewayConfig, FabricProperties |
| isFabricAvailable() E2E guard | Already exists | Phase 16 added it in test-env.ts |
| MockBlockchainService fallback | Already works | @Primary when fabric profile inactive |

## Scope Fences

- **IN SCOPE:** FabricCAService, conditional gateway identity, CA failure fallback, E2E CA test
- **OUT OF SCOPE:** Per-enterprise enrollment, BlockchainServicePort interface changes, CA admin API, TLS certificate rotation, new REST endpoints for CA operations

## Codebase Context

### Reusable Assets
- `FabricProperties.Ca` — config scaffold with 4 fields (lines 21-29 of FabricProperties.java)
- `FabricGatewayConfig` — existing gateway construction (lines 1-112), static crypto loading at lines ~70-90
- `docker-compose.fabric.yml` — CA service at lines 94-109, `hyperledger/fabric-ca:1.5.19`
- `isFabricAvailable()` — E2E guard in `test-env.ts` lines 38-50

### Files to Modify/Create
| File | Action | Purpose |
|------|--------|---------|
| `service/FabricCAService.java` | CREATE | CA enrollment logic |
| `config/FabricGatewayConfig.java` | MODIFY | Add conditional CA identity |
| `config/FabricProperties.java` | MODIFY | Add missing CA fields if needed |
| `pom.xml` | MODIFY | Add fabric-ca-sdk dependency if missing |
| `application-fabric.yml` | MODIFY | Add CA config properties |
| `blockchain-formula-flow.spec.ts` | MODIFY | Add CA enrollment test describe |

### Canonical Refs
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/FabricProperties.java` — CA config scaffold
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/FabricGatewayConfig.java` — Gateway construction
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/FabricBlockchainService.java` — Existing blockchain service
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/BlockchainServicePort.java` — Service interface (NOT modified)
- `docker-compose.fabric.yml` — CA Docker service definition

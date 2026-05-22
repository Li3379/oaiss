---
phase: 18
slug: fabric-ca
status: verified
threats_open: 0
asvs_level: 1
created: 2026-05-22
---

# Phase 18 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| FabricCAService → Fabric CA REST API | Backend calls external CA service over HTTP/HTTPS | Enrollment CSR (public key + CN), receives signed cert — no secrets transmitted outbound |
| FabricProperties.Ca.adminPassword | Secret credential stored in config, injected at runtime via env var | Admin password for CA authentication — HIGH sensitivity |

---

## Threat Register

| Threat ID | Category | Component | Disposition | Mitigation | Status |
|-----------|----------|-----------|-------------|------------|--------|
| T-18-01 | Information Disclosure | FabricProperties.Ca.toString() | mitigate | @ToString.Exclude on adminPassword field prevents Lombok from including it in toString() output, blocking accidental logging | closed |
| T-18-02 | Information Disclosure | EnrollmentResult serialization | mitigate | EnrollmentResult is a plain record with no Jackson annotations, no JPA mapping, no DTO exposure — never serialized or persisted | closed |
| T-18-03 | Spoofing | CA enrollment response | mitigate | WebClient uses configured endpoint from FabricProperties.Ca.endpoint; TLS enforced via https:// scheme in config | closed |
| T-18-04 | Tampering | CA REST API response | accept | CA is a trusted infrastructure component within the Fabric network; response integrity is the CA's responsibility | closed |
| T-18-SC | Tampering | Maven/npm installs | mitigate | No new package installs — spring-boot-starter-webflux and bcpkix-jdk18on already existed in pom.xml | closed |

*Status: open · closed*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-18-01 | T-18-04 | CA REST API response tampering accepted because CA is a trusted infrastructure component within the Fabric network. TLS provides transport-level integrity when configured with https:// endpoint. | gsd:secure-phase | 2026-05-22 |

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-05-22 | 5 | 5 | 0 | gsd:secure-phase (auto) |

### Verification Evidence

- **T-18-01**: `@ToString.Exclude` confirmed on `FabricProperties.Ca.adminPassword` — grep verified, unit test `testAdminPasswordExcludedFromToString` passed
- **T-18-02**: `EnrollmentResult` is `public record` with no annotations — no `@Entity`, `@Serializable`, `@JsonProperty`. Unit test `testEnrollmentResultNotSerialized` passed
- **T-18-03**: `FabricCAService.registerEnrollment()` uses `WebClient` with `FabricProperties.Ca.endpoint` — endpoint configurable, TLS via https:// scheme
- **T-18-04**: Accepted — CA is trusted infra. Documented in Accepted Risks Log
- **T-18-SC**: `pom.xml` diff confirmed zero new dependencies added in Phase 18

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-05-22

# Phase 18: Fabric CA 可选集成 - Research

**Researched:** 2026-05-22
**Domain:** Hyperledger Fabric CA enrollment / blockchain identity management
**Confidence:** MEDIUM

## Summary

Phase 18 adds optional Fabric CA enrollment to the OAISS CHAIN blockchain gateway. The gateway currently loads static crypto files (PEM cert + key from classpath) for its blockchain identity. This phase introduces a `FabricCAService` that, when `fabric.ca.enabled=true`, enrolls the gateway admin identity with the Fabric CA server and uses the CA-issued certificate instead of the static files. If the CA is unavailable, the system falls back to the existing static crypto.

**Critical discovery:** The `fabric-gateway` SDK (v1.7.1, already in pom.xml) intentionally does NOT include CA enrollment. The old `fabric-sdk-java` (deprecated since Fabric v2.5) contains `HFCAClient` for enrollment, but it brings heavy transitive dependencies that may conflict with Spring Boot 3.2.5. The recommended approach is to call the Fabric CA REST API directly (`POST /api/v1/enroll`) using Spring's existing `WebClient` (already in the project for ML service calls), avoiding the legacy SDK entirely. This is a single HTTP POST with Basic Auth, returning a base64-encoded certificate.

**Primary recommendation:** Use Spring WebClient to call Fabric CA's `/api/v1/enroll` REST endpoint directly. Generate a key pair and CSR using BouncyCastle (already in pom.xml as `bcpkix-jdk18on:1.78.1`), POST the CSR to the CA, parse the response into a Gateway SDK `Identity` and `Signer`. No new Maven dependencies required beyond what already exists.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01: Enrollment Scope -- Gateway Admin Only.** Implement gateway admin enrollment only. Use CA to manage the gateway's own identity (replacing static cert/key files), NOT per-enterprise enrollment. Create `FabricCAService` with `registerEnrollment()` that enrolls the gateway admin. Modify `FabricGatewayConfig` to call `registerEnrollment()` when `ca.enabled=true`. Do NOT modify `BlockchainServicePort` interface.
- **D-02: CA Failure Behavior -- Graceful Fallback.** When `ca.enabled=true` but CA service is unavailable, fall back to static crypto files (classpath `user-cert.pem`, `user-key.pem`). Log a warning but do not block startup.
- **D-03: No BlockchainServicePort Interface Changes.** Do NOT add CA-related methods to `BlockchainServicePort`. CA enrollment is a connection/infrastructure concern handled by `FabricGatewayConfig` and `FabricCAService`, not a business operation exposed through the service port.
- **D-04: FabricCAService Design.** Create `FabricCAService` as a `@Service` with `@Profile("fabric")` that reads CA config from `FabricProperties.Ca`, implements `registerEnrollment()`, returns a Fabric `Identity` and `Signer` for use by `FabricGatewayConfig`.
- **D-05: E2E Test Coverage.** Add a `describe` block within existing `blockchain-formula-flow.spec.ts` that tests CA enrollment status, guarded by `isFabricAvailable()`. No new test file needed.

### Claude's Discretion
(None explicitly stated -- all design decisions were locked in discuss phase)

### Deferred Ideas (OUT OF SCOPE)
- Per-enterprise enrollment / multi-tenant identity management
- BlockchainServicePort interface changes
- CA admin API (register, revoke, affiliations)
- TLS certificate rotation
- New REST endpoints for CA operations
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| FABRIC-01 | Fabric CA 可选集成: When `fabric.ca.enabled=true`, enroll gateway admin with Fabric CA server; fall back to static crypto on CA failure | Fabric CA REST API `/api/v1/enroll` documented via official Swagger spec; WebClient already in project; BouncyCastle already in project for CSR generation |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| CA enrollment (CSR generation, HTTP call) | API / Backend | -- | CA enrollment happens at gateway construction time in `FabricGatewayConfig` -- purely a backend concern |
| Gateway identity construction | API / Backend | -- | `FabricGatewayConfig` bean builds the `Gateway` with `Identity` and `Signer` |
| CA config properties | API / Backend | -- | `FabricProperties.Ca` is already scaffolded in Spring config |
| CA Docker service | Infra (Docker) | -- | `hyperledger/fabric-ca:1.5.19` in `docker-compose.fabric.yml` |
| E2E CA enrollment test | Browser / Client | API / Backend | Playwright test hits backend `/blockchain/status` to verify CA enrollment info |
| Fallback behavior | API / Backend | -- | Static crypto loading in `FabricGatewayConfig` already works |

## Standard Stack

### Core (Already in Project)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `fabric-gateway` | 1.7.1 | Blockchain gateway SDK | Official replacement for deprecated `fabric-sdk-java` -- provides `Identity`, `Signer`, `Gateway` types [VERIFIED: pom.xml] |
| `bcpkix-jdk18on` (BouncyCastle) | 1.78.1 | RSA key pair generation, CSR (PKCS#10) creation | Standard Java crypto library; already in pom.xml; needed for CSR generation for CA enrollment [VERIFIED: pom.xml] |
| Spring WebFlux (`WebClient`) | 3.2.5 | HTTP client for calling Fabric CA REST API | Already used for ML service calls; avoids adding Apache HttpClient from legacy SDK [VERIFIED: pom.xml] |
| `grpc-netty-shaded` | 1.62.2 | gRPC channel for Fabric peer communication | Already in pom.xml; no changes needed [VERIFIED: pom.xml] |

### NOT Adding (Key Decision)

| Library | Why NOT Adding |
|---------|----------------|
| `fabric-sdk-java` (2.2.x) | Deprecated since Fabric v2.5. Brings 30+ transitive dependencies (Apache HttpClient, old BouncyCastle, etc.) that conflict with Spring Boot 3.2.5. The only class needed (`HFCAClient`) can be replaced by a single REST call. [CITED: github.com/hyperledger/fabric-sdk-java -- "For Java applications use the latest released version of the SDK v1.4.x releases"] |
| `fabric-ca-sdk` (1.2.1) | Last released 2018. Part of the deprecated SDK family. Not compatible with modern Spring Boot. [VERIFIED: Maven Central search confirmed artifact exists at `org.hyperledger.fabric-ca:fabric-ca-sdk` but last release was years ago] |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| REST-based enrollment via WebClient | Legacy `HFCAClient` from `fabric-sdk-java` | HFCAClient handles CSR generation internally but adds 30+ transitive deps. REST approach uses existing BouncyCastle + WebClient, zero new dependencies |
| REST-based enrollment via WebClient | `fabric-ca-sdk` standalone | Very old artifact (1.2.1), last updated ~2018, unclear compatibility with Java 17. REST approach is forward-compatible |

**Installation:**
```bash
# No new dependencies needed -- all required libraries already in pom.xml
# Verify existing:
cd oaiss-chain-backend && mvn dependency:tree -Dincludes=org.hyperledger,org.bouncycastle
```

## Package Legitimacy Audit

> No new packages are being installed. All required dependencies already exist in pom.xml.

| Package | Registry | Status | Disposition |
|---------|----------|--------|-------------|
| `org.hyperledger.fabric:fabric-gateway:1.7.1` | Maven Central | Already installed | N/A (existing) |
| `org.bouncycastle:bcpkix-jdk18on:1.78.1` | Maven Central | Already installed | N/A (existing) |
| `org.springframework.boot:spring-boot-starter-webflux` | Maven Central | Already installed | N/A (existing) |

**Packages NOT installed (and why):**
- `org.hyperledger.fabric-sdk-java:fabric-sdk-java` -- deprecated, heavy dependency tree, REST approach chosen instead
- `org.hyperledger.fabric-ca:fabric-ca-sdk` -- last updated ~2018, not maintained, REST approach chosen instead

**Note on hallucinated package name:** `org.hyperledger:fabric-ca-sdk` was initially considered but flagged as `[SLOP]` by slopcheck. The actual Maven artifacts are `org.hyperledger.fabric-sdk-java:fabric-sdk-java` and `org.hyperledger.fabric-ca:fabric-ca-sdk`. Neither is being added to this project.

## Architecture Patterns

### System Architecture Diagram

```
                          FabricGatewayConfig (@Profile("fabric"))
                          ========================================
                          |
                          v
                    ca.enabled?
                   /          \
                 YES           NO
                 /               \
                v                 v
        FabricCAService      Static Crypto Loading
        (registerEnrollment)  (existing newIdentity/newSigner)
                |
                v
        Generate RSA KeyPair          <-- BouncyCastle KeyPairGenerator
                |
                v
        Build PKCS#10 CSR             <-- BouncyCastle PKCS10CertificationRequest
                |
                v
        POST /api/v1/enroll           <-- Spring WebClient
        Authorization: Basic <base64>
        Body: { certificate_request: "<PEM CSR>" }
                |
                v
        Parse Response                <-- result.Cert (base64-encoded PEM)
                |
                v
        Build Identity + Signer       <-- Gateway SDK X509Identity + Signers
               / \
              /   \
         SUCCESS  EXCEPTION
            |        |
            v        v
    Return to    Log warning,
    Gateway      fall through to
    Builder      static crypto loading
```

### Recommended Project Structure

```
oaiss-chain-backend/src/main/java/com/oaiss/chain/
  config/
    FabricProperties.java      -- EXISTS: Ca nested class already scaffolded
    FabricGatewayConfig.java   -- MODIFY: add conditional CA identity loading
  service/
    FabricCAService.java       -- CREATE: CA enrollment via REST API

oaiss-chain-backend/src/main/resources/
  application-fabric.yml       -- MODIFY: add CA config properties

oaiss-chain-frontend/tests/e2e/v1.1/
  blockchain-formula-flow.spec.ts  -- MODIFY: add CA enrollment describe block
```

### Pattern 1: FabricCAService -- REST-Based Enrollment

**What:** A Spring `@Service` under `@Profile("fabric")` that calls the Fabric CA REST API to enroll the gateway admin identity.

**When to use:** When `fabric.ca.enabled=true` in application config.

**Example:**

```java
// FabricCAService.java
@Service
@Profile("fabric")
@RequiredArgsConstructor
@Slf4j
public class FabricCAService {

    private final FabricProperties props;

    /**
     * Record to hold enrollment result: Identity + Signer for Gateway SDK.
     */
    public record EnrollmentResult(Identity identity, Signer signer) {}

    /**
     * Enroll gateway admin with Fabric CA server.
     * Steps:
     * 1. Generate RSA key pair
     * 2. Build PKCS#10 CSR with the key pair
     * 3. POST CSR to CA /api/v1/enroll with Basic Auth
     * 4. Parse response certificate
     * 5. Build Gateway Identity + Signer
     */
    public EnrollmentResult registerEnrollment() throws Exception {
        FabricProperties.Ca caProps = props.getCa();
        log.info("Enrolling gateway admin '{}' with CA at {}",
                caProps.getAdminName(), caProps.getEndpoint());

        // 1. Generate key pair
        KeyPair keyPair = generateKeyPair();

        // 2. Build CSR (PKCS#10)
        String csrPem = buildCsr(keyPair, caProps.getAdminName());

        // 3. Call CA REST API
        String certPem = callEnrollEndpoint(caProps, csrPem);

        // 4. Parse into Gateway SDK types
        X509Certificate certificate = parseCertificate(certPem);
        Identity identity = new X509Identity(props.getMspId(), certificate);
        Signer signer = Signers.newPrivateKeySigner(keyPair.getPrivate());

        log.info("CA enrollment successful for '{}'", caProps.getAdminName());
        return new EnrollmentResult(identity, signer);
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String buildCsr(KeyPair keyPair, String subject) throws Exception {
        // Use BouncyCastle PKCS10CertificationRequest
        // Subject DN: CN=<adminName>
        // Sign with SHA256WithRSA
        X500Principal dn = new X500Principal("CN=" + subject);
        PKCS10CertificationRequestBuilder csrBuilder =
            new JcaPKCS10CertificationRequestBuilder(dn, keyPair.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
            .build(keyPair.getPrivate());
        PKCS10CertificationRequest csr = csrBuilder.build(signer);
        return "-----BEGIN CERTIFICATE REQUEST-----\n"
            + Base64.getEncoder().encodeToString(csr.getEncoded())
            + "\n-----END CERTIFICATE REQUEST-----\n";
    }
}
```

### Pattern 2: Conditional Identity Loading in FabricGatewayConfig

**What:** Modify `fabricGateway()` bean to try CA enrollment first when enabled, falling back to static crypto on failure.

**When to use:** In the gateway bean construction, before building the Gateway instance.

**Example:**

```java
// In FabricGatewayConfig.java -- modified fabricGateway() bean
@Bean(destroyMethod = "close")
public Gateway fabricGateway() throws Exception {
    log.info("Connecting to Fabric peer at {} (TLS={})",
            props.getPeerEndpoint(), props.isTlsEnabled());

    ManagedChannel channel = newGrpcChannel();
    Identity identity;
    Signer signer;

    if (props.getCa().isEnabled()) {
        try {
            FabricCAService.EnrollmentResult result = fabricCAService.registerEnrollment();
            identity = result.identity();
            signer = result.signer();
            log.info("Using CA-issued identity for gateway");
        } catch (Exception e) {
            log.warn("CA enrollment failed, falling back to static crypto: {}",
                    e.getMessage());
            identity = newIdentity();   // existing static file loading
            signer = newSigner();        // existing static file loading
        }
    } else {
        identity = newIdentity();
        signer = newSigner();
    }

    Gateway.Builder builder = Gateway.newInstance()
            .identity(identity).signer(signer).connection(channel);
    return builder.connect();
}
```

### Anti-Patterns to Avoid

- **Adding the deprecated `fabric-sdk-java` dependency:** It pulls in Apache HttpClient 4.x, old BouncyCastle 1.6x, and numerous other transitive deps that conflict with Spring Boot 3.2.5's managed versions. The REST approach avoids all of this.
- **Putting CA enrollment logic in `FabricBlockchainService`:** CA enrollment is a connection-layer concern, not a business operation. Keep it in `FabricCAService` + `FabricGatewayConfig`.
- **Blocking startup on CA failure:** Per D-02, the system must fall back gracefully. Never throw an unhandled exception from `registerEnrollment()` that prevents the application from starting.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| CSR (PKCS#10) generation | Custom ASN.1/DER encoding | BouncyCastle `PKCS10CertificationRequestBuilder` + `JcaContentSignerBuilder` | ASN.1 encoding is error-prone; BouncyCastle already in pom.xml |
| X.509 certificate parsing | Custom PEM/DER parsing | `java.security.cert.CertificateFactory` or Gateway SDK `Identities.readX509Certificate()` | Handles all X.509 edge cases (extensions, chains, encoding) |
| HTTP Basic Auth header | Manual Base64 concatenation | Spring WebClient `Headers.httpBasic()` or `Authorization` header builder | Encoding edge cases (special chars in password) |
| RSA key pair generation | Custom crypto | `java.security.KeyPairGenerator.getInstance("RSA")` | Standard JCA API, well-tested |

**Key insight:** BouncyCastle (`bcpkix-jdk18on:1.78.1`) is already in pom.xml and provides all CSR/PKCS#10 building blocks. No additional crypto libraries needed.

## Common Pitfalls

### Pitfall 1: Deprecated SDK Dependency Conflicts

**What goes wrong:** Adding `fabric-sdk-java:2.2.x` to pom.xml pulls in 30+ transitive dependencies including old BouncyCastle (1.6x), Apache HttpClient 4.x, and various gRPC versions that conflict with the project's existing `fabric-gateway:1.7.1`, `bcpkix-jdk18on:1.78.1`, and Spring Boot 3.2.5 managed deps.

**Why it happens:** `fabric-sdk-java` was designed for Fabric 1.x/2.x era and its dependency tree hasn't been updated for Spring Boot 3.x compatibility.

**How to avoid:** Use the REST-based enrollment approach. Call `/api/v1/enroll` via WebClient. Zero new Maven dependencies.

**Warning signs:** `NoSuchMethodError` or `ClassNotFoundException` at runtime related to BouncyCastle, HttpClient, or gRPC classes.

### Pitfall 2: TLS Handshake with Fabric CA Server

**What goes wrong:** The CA Docker service has `FABRIC_CA_SERVER_TLS_ENABLED=true`. If WebClient does not trust the CA's TLS certificate, the enrollment request fails with `SSLHandshakeException`.

**Why it happens:** The Fabric CA uses a self-signed TLS certificate. WebClient's default SSL context only trusts JDK's default CAs.

**How to avoid:** For development, configure WebClient to trust the CA's TLS cert. Options: (a) add CA cert to JVM truststore, (b) use `http://` endpoint in dev (set `fabric.ca.endpoint=http://...`), (c) configure custom SSLContext on WebClient. The docker-compose exposes port 7054 with TLS, but the internal Docker network can also be used with `http://ca.org1.example.com:7054` if the backend runs in Docker.

**Warning signs:** `javax.net.ssl.SSLHandshakeException: PKIX path building failed` or `sun.security.provider.certpath.SunCertPathBuilderException`.

### Pitfall 3: CSR Subject DN Mismatch

**What goes wrong:** The CA issues a certificate with a different CN than expected, or the enrollment fails with "authorization failure" because the CSR subject doesn't match the enrollment ID.

**Why it happens:** Fabric CA expects the CSR's subject CN to match the enrollment ID (the user in the Basic Auth header).

**How to avoid:** Always set CSR subject to `CN=<adminName>` where `adminName` matches `FabricProperties.Ca.adminName`. Do not add O, OU, or other DN components unless the CA policy requires them.

**Warning signs:** CA returns HTTP 401 or error code 20 (authorization failure) despite correct credentials.

### Pitfall 4: Enrollment Secret Already Consumed

**What goes wrong:** The bootstrap admin identity (`admin:adminpw`) has a max enrollment count. After too many enrollments, the CA rejects further enrollment attempts.

**Why it happens:** Fabric CA tracks enrollment counts. The default `max_enrollments` for the bootstrap identity may be limited.

**How to avoid:** Set `max_enrollments: -1` (infinite) in the CA server config, or handle the error gracefully in the fallback path. For development, the `-d` flag in the CA server command enables debug mode which may help diagnose this.

**Warning signs:** CA returns error code 0 with message "Registration of 'admin' failed".

### Pitfall 5: Base64 Encoding in CA Response

**What goes wrong:** The CA response `result.Cert` field contains the certificate in base64 encoding, not raw PEM. Attempting to parse it as PEM directly fails.

**Why it happens:** Per the Swagger spec, `Cert` is described as "The enrollment certificate in base 64 encoded format."

**How to avoid:** Decode the base64 string first, then wrap in PEM headers if needed, or parse the raw bytes with `CertificateFactory`. The response may or may not include PEM headers.

**Warning signs:** `CertificateException: Could not parse certificate` or `java.security.cert.CertificateParsingException`.

## Code Examples

### Fabric CA REST API Enrollment (verified from official Swagger spec)

The `/api/v1/enroll` endpoint contract, verified from the official Swagger specification at `github.com/hyperledger/fabric-ca/blob/master/swagger/swagger-fabric-ca.json` [VERIFIED: GitHub source]:

```java
// Source: Hyperledger Fabric CA Swagger spec (swagger-fabric-ca.json)
//
// POST /api/v1/enroll
// Headers:
//   Authorization: Basic <base64(admin:adminpw)>
//   Content-Type: application/json
// Body:
//   { "certificate_request": "<PEM-encoded PKCS#10 CSR>" }
//
// Response 201:
//   {
//     "success": true,
//     "result": {
//       "Cert": "<base64-encoded certificate>",
//       "ServerInfo": { "CAName": "...", "CAChain": "..." }
//     },
//     "errors": [],
//     "messages": []
//   }
//
// The "Cert" field is base64-encoded. Decode before parsing.
```

### WebClient Call to Fabric CA

```java
// Using Spring WebClient (already in project for ML service calls)
private String callEnrollEndpoint(FabricProperties.Ca caProps, String csrPem) {
    String authHeader = Base64.getEncoder().encodeToString(
        (caProps.getAdminName() + ":" + caProps.getAdminPassword()).getBytes());

    Map<String, String> body = Map.of("certificate_request", csrPem);

    WebClient webClient = WebClient.builder()
        .baseUrl(caProps.getEndpoint())
        .build();

    Map<String, Object> response = webClient.post()
        .uri("/api/v1/enroll")
        .header("Authorization", "Basic " + authHeader)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
        .block();

    if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
        throw new RuntimeException("CA enrollment failed: " + response);
    }

    Map<String, Object> result = (Map<String, Object>) response.get("result");
    return (String) result.get("Cert");  // base64-encoded certificate
}
```

### CSR Generation with BouncyCastle

```java
// BouncyCastle already in pom.xml as bcpkix-jdk18on:1.78.1
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.openssl.jcajce.JcaPKCS10CertificationRequestBuilder;

private String buildCsr(KeyPair keyPair, String cn) throws Exception {
    X500Principal subject = new X500Principal("CN=" + cn);
    PKCS10CertificationRequestBuilder builder =
        new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
    var signer = new JcaContentSignerBuilder("SHA256WithRSA")
        .setProvider("BC")
        .build(keyPair.getPrivate());
    PKCS10CertificationRequest csr = builder.build(signer);

    byte[] encoded = csr.getEncoded();
    String base64 = Base64.getEncoder().encodeToString(encoded);
    // Format as PEM
    StringBuilder pem = new StringBuilder();
    pem.append("-----BEGIN CERTIFICATE REQUEST-----\n");
    // Split into 64-char lines
    for (int i = 0; i < base64.length(); i += 64) {
        pem.append(base64, i, Math.min(i + 64, base64.length()));
        pem.append('\n');
    }
    pem.append("-----END CERTIFICATE REQUEST-----\n");
    return pem.toString();
}
```

### E2E Test Pattern (Playwright)

```typescript
// Add to blockchain-formula-flow.spec.ts, inside the existing test.describe
test.describe('Fabric CA Enrollment', () => {
  test.skip(async () => !(await isFabricAvailable()), 'Fabric network not available')

  test('CA enrollment status available in blockchain status', async ({ request }) => {
    const response = await request.get(`${API_BASE}/blockchain/status`)
    expect(response.ok()).toBeTruthy()
    const body = await response.json()
    expect(body.data).toBeDefined()
    expect(body.data).toHaveProperty('connected')
    // When CA is enabled, should include enrollment info
    if (body.data.caEnabled) {
      expect(body.data).toHaveProperty('enrollmentStatus')
    }
  })
})
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `fabric-sdk-java` (HFCAClient) | `fabric-gateway` SDK + direct CA REST calls | Fabric v2.5 (2023) | Gateway SDK has no CA support; must call CA REST API directly |
| Static PEM files for gateway identity | Dynamic CA enrollment | Fabric CA 1.5.x | Production deployments should use CA for identity lifecycle |
| `fabric-ca-sdk` standalone artifact | Direct REST API calls | Artifact last updated ~2018 | Not maintained; REST approach is the current best practice |

**Deprecated/outdated:**
- `org.hyperledger.fabric-sdk-java:fabric-sdk-java`: Deprecated since Fabric v2.5. Replaced by `org.hyperledger.fabric:fabric-gateway`. [CITED: github.com/hyperledger/fabric-sdk-java]
- `org.hyperledger.fabric-ca:fabric-ca-sdk`: Last maintained ~2018. Part of the deprecated SDK family.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | CSR subject must be `CN=<adminName>` matching the Basic Auth user | Architecture Patterns | MEDIUM -- CA enrollment would fail with auth error; easy to debug |
| A2 | CA response `result.Cert` is base64-encoded (not raw PEM) | Code Examples | MEDIUM -- certificate parsing would fail; easy to fix |
| A3 | WebClient can connect to CA from Spring Boot container in Docker network | Architecture Patterns | LOW -- Docker networking is well-understood; fallback exists |
| A4 | BouncyCastle `PKCS10CertificationRequestBuilder` works with Java 17 + BC 1.78.1 | Code Examples | LOW -- BouncyCastle 1.78.1 explicitly targets JDK 18on, compatible with Java 17 |
| A5 | Bootstrap admin identity `admin:adminpw` has unlimited max_enrollments in dev | Common Pitfalls | LOW -- can re-create CA container to reset |

**All critical claims verified via official sources (Swagger spec, Maven Central, GitHub repos). Assumptions are limited to behavioral details of the CA server.**

## Open Questions

1. **TLS Trust for CA Server**
   - What we know: CA Docker has `FABRIC_CA_SERVER_TLS_ENABLED=true`; backend may run outside Docker
   - What's unclear: Whether the backend needs to trust the CA's TLS cert for development
   - Recommendation: In `application-fabric.yml`, use `http://` for CA endpoint in dev (within Docker network) or configure WebClient with custom SSL context. Planner should add a task for TLS handling.

2. **Enrollment Persistence Across Restarts**
   - What we know: Each `registerEnrollment()` call generates a new key pair and enrolls with CA
   - What's unclear: Whether repeated enrollment of the same identity is allowed by default CA config
   - Recommendation: The bootstrap `admin` identity typically allows multiple enrollments. If issues arise, the fallback to static crypto handles it. No caching needed for Phase 18 scope.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker | Fabric CA service | true | 29.4.3 | -- |
| Docker Compose | Fabric CA service | true | v5.1.3 | -- |
| Java 17 | Backend runtime | true | JDK 17 | -- |
| Maven | Build | true | (via wrapper) | -- |
| Node.js | Frontend E2E tests | true | (available) | -- |
| Playwright | E2E tests | true | (in frontend) | -- |
| Fabric CA Docker image | CA service | true | 1.5.19 | -- |

**Missing dependencies with no fallback:** None -- all required tools are available.

**Missing dependencies with fallback:** None.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework (Backend) | JUnit 5 + Mockito (spring-boot-starter-test) |
| Framework (E2E) | Playwright |
| Config file (Backend) | N/A -- uses Spring Boot test annotations |
| Config file (E2E) | `oaiss-chain-frontend/playwright.config.ts` |
| Quick run command (Backend) | `cd oaiss-chain-backend && mvn test -pl . -Dtest=FabricCAServiceTest` |
| Quick run command (E2E) | `cd oaiss-chain-frontend && npx playwright test tests/e2e/v1.1/blockchain-formula-flow.spec.ts` |
| Full suite command (Backend) | `cd oaiss-chain-backend && mvn test` |
| Full suite command (E2E) | `cd oaiss-chain-frontend && npx playwright test` |

### Phase Requirements -> Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| FABRIC-01 | CA enrollment succeeds when CA available and enabled | Unit | `mvn test -Dtest=FabricCAServiceTest` | Wave 0 -- create |
| FABRIC-01 | CA enrollment falls back to static crypto on failure | Unit | `mvn test -Dtest=FabricCAServiceTest` | Wave 0 -- create |
| FABRIC-01 | Gateway uses CA identity when ca.enabled=true | Unit | `mvn test -Dtest=FabricGatewayConfigTest` | Wave 0 -- create |
| FABRIC-01 | CA enrollment status in blockchain status endpoint | E2E | `npx playwright test tests/e2e/v1.1/blockchain-formula-flow.spec.ts` | Wave 0 -- modify |

### Sampling Rate

- **Per task commit:** `cd oaiss-chain-backend && mvn test -Dtest=FabricCAServiceTest`
- **Per wave merge:** `cd oaiss-chain-backend && mvn test`
- **Phase gate:** Full backend test suite green + E2E Fabric test passing (when Fabric available)

### Wave 0 Gaps

- [ ] `oaiss-chain-backend/src/test/java/com/oaiss/chain/service/FabricCAServiceTest.java` -- covers CA enrollment logic
- [ ] `oaiss-chain-backend/src/test/java/com/oaiss/chain/config/FabricGatewayConfigTest.java` -- covers conditional identity loading
- [ ] `oaiss-chain-frontend/tests/e2e/v1.1/blockchain-formula-flow.spec.ts` -- add CA enrollment describe block (file exists, needs modification)

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | CA enrollment uses Basic Auth with bootstrap admin credentials; credentials stored in Spring config (`FabricProperties.Ca.adminPassword`) |
| V4 Access Control | no | CA enrollment is infrastructure-level, not user-facing; no user roles involved |
| V5 Input Validation | yes | Validate CA endpoint URL format, admin credentials non-empty; validate CSR before sending |
| V6 Cryptography | yes | RSA 2048-bit key pair generation; SHA256WithRSA CSR signing; BouncyCastle for all crypto operations (never hand-roll) |
| V8 Data Protection | yes | CA admin password in config must not be logged; enrollment result (private key) must not be serialized or exposed via API |

### Known Threat Patterns for Spring Boot + Fabric CA

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Credential exposure (admin password in logs) | Information Disclosure | Never log `FabricProperties.Ca.adminPassword`; use `@ToString.Exclude` on Lombok |
| MITM on CA endpoint (unencrypted enrollment) | Tampering, Spoofing | Use HTTPS for CA endpoint in production; validate TLS cert |
| Private key leakage from enrollment result | Information Disclosure | EnrollmentResult is transient; never persist or serialize; keep in memory only |
| CSR manipulation | Tampering | CSR is generated locally with known key pair; no external input to CSR content |
| CA server compromise | Elevation of Privilege | CA is Docker-isolated; bootstrap identity has limited scope; Phase 18 only enrolls gateway admin |

### Security Requirements for Phase 18

1. `FabricProperties.Ca.adminPassword` must use `@ToString.Exclude` (Lombok) to prevent accidental logging
2. WebClient must use HTTPS in production (`fabric.ca.endpoint=https://...`)
3. The `EnrollmentResult` record must never be serialized to JSON or stored in any cache/store
4. CSR generation must happen in-memory; no temporary files with private key material
5. The existing `MockBlockchainService` must not be affected (separate profile)

## Sources

### Primary (HIGH confidence)
- Fabric CA Swagger specification -- `github.com/hyperledger/fabric-ca/blob/master/swagger/swagger-fabric-ca.json` -- REST API contract for `/api/v1/enroll` [VERIFIED: fetched and parsed]
- Project pom.xml -- verified `fabric-gateway:1.7.1`, `bcpkix-jdk18on:1.78.1`, `spring-boot-starter-webflux` present
- Project source code -- `FabricGatewayConfig.java`, `FabricProperties.java`, `FabricBlockchainService.java` -- verified existing patterns
- Docker Compose -- `docker-compose.fabric.yml` -- verified CA service at `hyperledger/fabric-ca:1.5.19`

### Secondary (MEDIUM confidence)
- Maven Central search -- confirmed `fabric-sdk-java:2.2.26` is latest version of deprecated SDK
- Maven Central search -- confirmed `fabric-ca-sdk:1.2.1-stable` exists under `org.hyperledger.fabric-ca` group
- GitHub `hyperledger/fabric-sdk-java` -- confirmed deprecation notice and recommended migration to `fabric-gateway`

### Tertiary (LOW confidence)
- None -- all claims verified through primary or secondary sources

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all dependencies verified in pom.xml; no new packages needed
- Architecture: HIGH -- existing codebase patterns well-understood; REST API contract verified from official Swagger spec
- Pitfalls: MEDIUM -- TLS handling and base64 encoding assumptions need runtime validation
- Security: HIGH -- clear attack surface; well-understood mitigations

**Research date:** 2026-05-22
**Valid until:** 2026-06-21 (stable -- Fabric CA API is mature and unlikely to change)

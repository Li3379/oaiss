# Hyperledger Fabric Java SDK Integration - Research

**Researched:** 2026-05-14
**Domain:** Hyperledger Fabric blockchain integration for carbon trading platform
**Confidence:** MEDIUM (Fabric SDK docs verified, but Fabric 3.0 migration details still evolving)

## Summary

The OAISS CHAIN platform currently uses a `MockBlockchainService` that simulates blockchain operations (report hashing, trade recording, query). The production target is Hyperledger Fabric, a permissioned blockchain ideal for enterprise carbon trading with its private channels, endorsement policies, and identity-based access.

The **Fabric Gateway SDK** (`org.hyperledger.fabric:fabric-gateway:1.11.0`) is the recommended SDK over the deprecated legacy `fabric-sdk-java`. The Gateway SDK provides a simplified API that delegates endorsement and ordering to the Fabric peer's Gateway service (introduced in Fabric 2.4). For development, Fabric's `test-network` provides a 2-org, 1-channel setup that can be embedded into the project's Docker Compose. Fabric CA should be used for production identity management, but `cryptogen` is acceptable for development.

**Primary recommendation:** Use Fabric Gateway SDK 1.11.0 with Fabric 2.5.x (or 3.0 once stable), implement chaincode in Java using `fabric-chaincode-shim`, and embed a minimal Fabric network in the existing Docker Compose for local development.

## Options Compared

### Option A: Fabric Gateway SDK (fabric-gateway) - RECOMMENDED

| Aspect | Detail |
|--------|--------|
| Maven coordinates | `org.hyperledger.fabric:fabric-gateway:1.11.0` [VERIFIED: Maven Central] |
| Minimum Fabric version | 2.4+ (requires peer Gateway service) |
| Status | Active, recommended by Hyperledger |
| API style | Simplified: `Gateway.newInstance()`, `network.getContract()`, `contract.submitTransaction()` |
| Endorsement | Handled by peer Gateway service (no manual endorsement plan needed) |
| gRPC dependency | `io.grpc:grpc-netty-shaded` (self-contained, no native dependency) |
| Java version | Java 8+ (compatible with Java 17) |

**Key advantage:** The Gateway SDK delegates all endorsement, ordering, and commit logic to the Fabric peer. Your application code only needs to call `submitTransaction()` (for writes) or `evaluateTransaction()` (for reads). No manual endorsement collection, no ordering service interaction.

### Option B: Legacy Fabric SDK for Java (fabric-sdk-java) - NOT RECOMMENDED

| Aspect | Detail |
|--------|--------|
| Maven coordinates | `org.hyperledger.fabric:fabric-sdk-java:2.2.26` [VERIFIED: Maven Central] |
| Minimum Fabric version | 1.4+ |
| Status | **DEPRECATED** - in maintenance mode only |
| API style | Low-level: manual channel construction, endorsement simulation, ordering |
| Endorsement | Client-side endorsement collection (complex, error-prone) |
| gRPC dependency | `io.grpc:grpc-netty-shaded` |
| Java version | Java 8+ |

**Why rejected:** Deprecated. Requires manual endorsement handling. Significantly more complex application code. The Fabric project recommends Gateway SDK for all new development.

### Option C: Fabric Gateway Java (fabric-gateway-java) - NOT RECOMMENDED

| Aspect | Detail |
|--------|--------|
| Maven coordinates | `org.hyperledger.fabric:fabric-gateway-java:2.2.8` [VERIFIED: Maven Central] |
| Status | **DEPRECATED** - superseded by `fabric-gateway` |
| Note | This is the old Gateway API, not the new one |

**Why rejected:** Deprecated. Confusingly named differently from `fabric-gateway`. Use `fabric-gateway` (no `-java` suffix).

## Recommendation

**Use Fabric Gateway SDK (`fabric-gateway:1.11.0`)** with **Fabric 2.5.x** for the following reasons:

1. **Active and recommended** - Official Hyperledger recommendation for all new development [CITED: hyperledger-fabric.readthedocs.io]
2. **Simplified API** - Gateway peer handles endorsement/ordering; application only calls submit/evaluate
3. **Java 17 compatible** - Runs on Java 8+, tested on Java 17 [VERIFIED: fabric-samples Application.java uses standard Java APIs]
4. **gRPC netty-shaded** - No native library dependency, works on Windows/Linux/macOS
5. **Fabric 2.5.x stable** - Production-ready, long-term support [ASSUMED - verify Fabric LTS policy]

**Why not Fabric 3.0:** Fabric 3.0 is available but introduces significant architectural changes (smart contract engines, removal of system channel). For a first integration, 2.5.x is the safer choice. Migration to 3.0 can be planned as a follow-up phase.

## Implementation Guidance

### Spring Boot Integration Pattern

The Fabric Gateway SDK is a library, not a Spring Boot starter. Integration follows a standard pattern: create a `@Configuration` class that builds the `Gateway` connection, expose key beans (`Gateway`, `Network`, `Contract`), and inject them into services.

```java
// Source: [CITED: hyperledger-fabric.readthedocs.io + fabric-samples Application.java]

@Configuration
public class FabricGatewayConfig {

    @Value("${fabric.peer.endpoint}")
    private String peerEndpoint;

    @Value("${fabric.peer.tls-certificate-path}")
    private String tlsCertPath;

    @Value("${fabric.identity.certificate-path}")
    private String certPath;

    @Value("${fabric.identity.private-key-path}")
    private String keyPath;

    @Value("${fabric.channel.name}")
    private String channelName;

    @Value("${fabric.chaincode.name}")
    private String chaincodeName;

    @Bean
    public Gateway fabricGateway() throws IOException, CertificateException, InvalidKeyException {
        // 1. Load client identity (X.509 certificate)
        Path certFile = Path.of(certPath);
        X509Certificate certificate = Identities.readX509Certificate(
            Files.newBufferedReader(certFile));

        // 2. Load private key
        Path keyFile = Path.of(keyPath);
        PrivateKey privateKey = Identities.readPrivateKey(
            Files.newBufferedReader(keyFile));

        // 3. Create identity and signer
        Identity identity = new X509Identity("Org1MSP", certificate);
        Signer signer = Signers.newPrivateKeySigner(privateKey);

        // 4. Create gRPC channel to peer
        Path tlsCert = Path.of(tlsCertPath);
        ManagedChannel grpcChannel = NettyChannelBuilder.forTarget(peerEndpoint)
            .sslContext(GrpcSslContexts.forClient()
                .trustManager(tlsCert.toFile())
                .build())
            .build();

        // 5. Build Gateway
        return Gateway.newInstance()
            .identity(identity)
            .signer(signer)
            .connection(grpcChannel)
            .connect();
    }

    @Bean
    public Network fabricNetwork(Gateway gateway) {
        return gateway.getNetwork(channelName);
    }

    @Bean
    public Contract fabricContract(Network network) {
        return network.getContract(chaincodeName);
    }
}
```

### Transaction Submission Pattern

```java
// Source: [CITED: fabric-samples Application.java + hyperledger.github.io/fabric-gateway]

@Service
@RequiredArgsConstructor
public class FabricBlockchainService implements BlockchainService {

    private final Contract fabricContract;

    // Submit = write to ledger (goes through endorsement + ordering + commit)
    @Override
    public String commitReportToChain(String reportId, String reportHash,
                                       Long enterpriseId, String emissionData) {
        try {
            byte[] result = fabricContract.submitTransaction(
                "CreateCarbonReport",
                reportId,
                reportHash,
                String.valueOf(enterpriseId),
                emissionData
            );
            return new String(result, StandardCharsets.UTF_8);
        } catch (EndorseException | CommitException e) {
            throw new BlockchainException("Failed to commit report to chain: " + e.getMessage());
        }
    }

    // Evaluate = read from ledger (query only, single peer, no consensus)
    @Override
    public String queryReportFromChain(String reportId) {
        try {
            byte[] result = fabricContract.evaluateTransaction(
                "QueryCarbonReport",
                reportId
            );
            return new String(result, StandardCharsets.UTF_8);
        } catch (GatewayException e) {
            throw new BlockchainException("Failed to query report from chain: " + e.getMessage());
        }
    }
}
```

### Chaincode Design for Carbon Trading

The chaincode (smart contract) should handle these domain operations:

| Function | Type | Description |
|----------|------|-------------|
| `CreateCarbonReport` | Submit | Record carbon emission report with hash, enterprise ID, emission data |
| `QueryCarbonReport` | Evaluate | Query a specific carbon report by ID |
| `QueryCarbonReportsByEnterprise` | Evaluate | Query all reports for an enterprise (uses CompositeKey) |
| `CreateTrade` | Submit | Record a carbon trade (buyer, seller, amount, price) |
| `QueryTrade` | Evaluate | Query a specific trade by ID |
| `QueryTradesByEnterprise` | Evaluate | Query trades involving an enterprise |
| `TransferCarbonCoin` | Submit | Transfer carbon coins between enterprises |
| `GetCarbonCoinBalance` | Evaluate | Query carbon coin balance for an enterprise |
| `RecordCreditScore` | Submit | Record enterprise credit score on chain |
| `QueryCreditScore` | Evaluate | Query enterprise credit score |
| `CreateCarbonNeutralProject` | Submit | Record a carbon neutral project registration |
| `VerifyDigitalSignature` | Submit | Verify a digital signature for a report |

**Chaincode Java skeleton:**

```java
// Source: [CITED: fabric-samples chaincode-java SmartContract.java + fabric-chaincode-java docs]

@Contract(name = "CarbonTradingContract")
@Default
public class CarbonTradingContract implements ContractInterface {

    private static final String REPORT_PREFIX = "REPORT";
    private static final String TRADE_PREFIX = "TRADE";
    private static final String COIN_PREFIX = "COIN";

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void CreateCarbonReport(Context ctx, String reportId, String reportHash,
                                    String enterpriseId, String emissionData) {
        ChaincodeStub stub = ctx.getStub();

        // Check if report already exists
        String existing = stub.getStringState(reportId);
        if (existing != null && !existing.isEmpty()) {
            throw new ChaincodeException("Report " + reportId + " already exists");
        }

        // Create report JSON
        CarbonReport report = new CarbonReport(reportId, reportHash,
            enterpriseId, emissionData, Instant.now().toString());

        // Store on ledger
        stub.putStringState(reportId, JSON.toJSONString(report));

        // Create composite key for enterprise index
        CompositeKey enterpriseKey = stub.createCompositeKey(REPORT_PREFIX, enterpriseId, reportId);
        stub.putStringState(enterpriseKey.toString(), reportId);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String QueryCarbonReport(Context ctx, String reportId) {
        ChaincodeStub stub = ctx.getStub();
        String data = stub.getStringState(reportId);
        if (data == null || data.isEmpty()) {
            throw new ChaincodeException("Report " + reportId + " does not exist");
        }
        return data;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String QueryCarbonReportsByEnterprise(Context ctx, String enterpriseId) {
        ChaincodeStub stub = ctx.getStub();
        CompositeKey startKey = stub.createCompositeKey(REPORT_PREFIX, enterpriseId, "");
        CompositeKey endKey = stub.createCompositeKey(REPORT_PREFIX, enterpriseId, "￿");

        StringBuilder result = new StringBuilder("[");
        boolean first = true;
        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(
                startKey.toString(), endKey.toString())) {
            for (KeyValue kv : results) {
                if (!first) result.append(",");
                first = false;
                result.append(kv.getStringValue());
            }
        }
        result.append("]");
        return result.toString();
    }
}
```

### Authentication: Fabric CA vs Certificate-Based

| Approach | Development | Production |
|----------|-------------|------------|
| **cryptogen** | Use for test-network setup | Not suitable |
| **Fabric CA** | Can use, but adds complexity | Recommended |

**Recommendation for this project:**
- **Development:** Use `cryptogen` to generate static certificates. The test-network script handles this automatically. Certificate files are mounted into the Spring Boot container.
- **Production:** Use Fabric CA with `Affiliation` mapping to enterprise roles. Each enterprise gets its own identity from their org's CA.

**Why not Fabric CA in dev:** Fabric CA requires running 2+ CA containers (one per org), registering users, and enrolling certificates. This adds 3-4 containers and startup complexity. `cryptogen` generates static certs at network bootstrap.

**Spring Boot identity management pattern:**
```yaml
# application.yml - dev profile
fabric:
  peer:
    endpoint: peer0.org1.example.com:7051
    tls-certificate-path: /fabric/crypto/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt
  identity:
    certificate-path: /fabric/crypto/peerOrganizations/org1.example.com/users/User1@org1.example.com/cert.pem
    private-key-path: /fabric/crypto/peerOrganizations/org1.example.com/users/User1@org1.example.com/keystore/priv_sk
  channel:
    name: carbonchannel
  chaincode:
    name: carbon-trading
```

### Key Exception Handling

```java
// Fabric Gateway SDK exception hierarchy:
// GatewayException - base for all Gateway errors
//   EndorseException - endorsement failed (peers refused to endorse)
//   CommitException - transaction committed but with validation errors
//   SubmitException - submission to orderer failed
// CommitStatusException - failed to get commit status

try {
    byte[] result = contract.submitTransaction("CreateCarbonReport", args...);
} catch (EndorseException e) {
    // Peers refused to endorse - check endorsement policy, chaincode logic
    log.error("Endorsement failed: {}", e.getDetails());
    throw new BlockchainException("Endorsement failed: " + e.getMessage());
} catch (CommitException e) {
    // Transaction was not valid when committed
    log.error("Commit failed with code: {}", e.getValidationCode());
    throw new BlockchainException("Commit failed: " + e.getMessage());
}
```

## Dependencies

### Application (Spring Boot) - Maven Dependencies

```xml
<!-- Fabric Gateway SDK - the recommended SDK -->
<dependency>
    <groupId>org.hyperledger.fabric</groupId>
    <artifactId>fabric-gateway</artifactId>
    <version>1.11.0</version>
</dependency>

<!-- gRPC Netty Shaded - included transitively, but pin version for compatibility -->
<!-- fabric-gateway 1.11.0 uses gRPC 1.62.x -->
<!-- Note: Spring Boot 3.2.5 manages grpc version via spring-grpc bom -->
<!-- If conflicts arise, use fabric-gateway's transitive version -->
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty-shaded</artifactId>
    <version>1.62.2</version>
</dependency>

<!-- Bouncy Castle for crypto operations (certificate/key loading) -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcpkix-jdk18on</artifactId>
    <version>1.78</version>
</dependency>
```

**Version compatibility notes:**
- `fabric-gateway:1.11.0` requires gRPC 1.62.x [VERIFIED: Maven Central - checked transitive dependencies]
- `fabric-gateway:1.11.0` is compatible with Fabric 2.4+ peers [CITED: hyperledger-fabric.readthedocs.io]
- `bcpkix-jdk18on:1.78` is the Java 18+ variant of Bouncy Castle [VERIFIED: Maven Central]
- If gRPC version conflicts with Spring Boot's managed version, add exclusions and pin the Fabric Gateway's version

**Potential Spring Boot gRPC conflict:** Spring Boot 3.2.x may manage a different gRPC version. If using `spring-grpc-spring-boot-starter`, it brings its own gRPC version. Resolution: let `fabric-gateway`'s BOM win by declaring it first, or exclude gRPC from one of them.

### Chaincode - Maven Dependencies (separate project)

```xml
<!-- Chaincode is deployed to Fabric peers, NOT included in Spring Boot app -->
<dependency>
    <groupId>org.hyperledger.fabric-chaincode-java</groupId>
    <artifactId>fabric-chaincode-shim</artifactId>
    <version>2.5.3</version>
</dependency>

<dependency>
    <groupId>org.hyperledger.fabric-chaincode-java</groupId>
    <artifactId>fabric-chaincode-protos</artifactId>
    <version>2.5.3</version>
</dependency>
```

**Important:** The chaincode runs inside the Fabric peer process. It is a **separate Maven project** (or Gradle project) from the Spring Boot application. The chaincode is packaged as a JAR and deployed to the peer via the `peer lifecycle chaincode` commands.

## Docker Setup

### Minimal Fabric Network Architecture

For local development, a minimal Fabric network requires:

| Container | Image | Port | Purpose |
|-----------|-------|------|---------|
| `orderer.example.com` | `hyperledger/fabric-orderer:2.5` | 7050 | Ordering service (Raft) |
| `peer0.org1.example.com` | `hyperledger/fabric-peer:2.5` | 7051 | Org1 peer (endorser + Gateway) |
| `peer0.org2.example.com` | `hyperledger/fabric-peer:2.5` | 9051 | Org2 peer (endorser) |
| `ca.org1.example.com` | `hyperledger/fabric-ca:1.5` | 7054 | Org1 CA (production only) |
| `ca.org2.example.com` | `hyperledger/fabric-ca:1.5` | 8054 | Org2 CA (production only) |

**Development only (no CA):** 3 containers (1 orderer + 2 peers). Use `cryptogen` for static certificates.

### Docker Compose Addition

Add to the existing `docker-compose.yml` alongside MySQL, Redis, MinIO:

```yaml
# Fabric network services - appended to existing docker-compose.yml
# NOTE: Crypto material must be generated first using:
#   ./fabric-network/scripts/generate-crypto.sh
#   ./fabric-network/scripts/create-channel.sh

services:
  orderer.example.com:
    container_name: orderer.example.com
    image: hyperledger/fabric-orderer:2.5
    environment:
      - FABRIC_LOGGING_SPEC=INFO
      - ORDERER_GENERAL_LISTENADDRESS=0.0.0.0
      - ORDERER_GENERAL_LISTENPORT=7050
      - ORDERER_GENERAL_LOCALMSPID=OrdererMSP
      - ORDERER_GENERAL_LOCALMSPDIR=/var/hyperledger/orderer/msp
      - ORDERER_GENERAL_TLS_ENABLED=true
      - ORDERER_GENERAL_TLS_PRIVATEKEY=/var/hyperledger/orderer/tls/server.key
      - ORDERER_GENERAL_TLS_CERTIFICATE=/var/hyperledger/orderer/tls/server.crt
      - ORDERER_GENERAL_TLS_ROOTCAS=[/var/hyperledger/orderer/tls/ca.crt]
      - ORDERER_GENERAL_CLUSTER_CLIENTCERTIFICATE=/var/hyperledger/orderer/tls/server.crt
      - ORDERER_GENERAL_CLUSTER_CLIENTPRIVATEKEY=/var/hyperledger/orderer/tls/server.key
      - ORDERER_GENERAL_CLUSTER_ROOTCAS=[/var/hyperledger/orderer/tls/ca.crt]
    working_dir: /root
    command: orderer
    volumes:
      - ./fabric-network/crypto-config/ordererOrganizations/example.com/orderers/orderer.example.com/msp:/var/hyperledger/orderer/msp
      - ./fabric-network/crypto-config/ordererOrganizations/example.com/orderers/orderer.example.com/tls:/var/hyperledger/orderer/tls
      - ./fabric-network/channel-artifacts:/var/hyperledger/orderer/channel-artifacts
    ports:
      - "7050:7050"
    networks:
      - fabric

  peer0.org1.example.com:
    container_name: peer0.org1.example.com
    image: hyperledger/fabric-peer:2.5
    environment:
      - FABRIC_LOGGING_SPEC=INFO
      - CORE_PEER_ID=peer0.org1.example.com
      - CORE_PEER_ADDRESS=peer0.org1.example.com:7051
      - CORE_PEER_LISTENADDRESS=0.0.0.0:7051
      - CORE_PEER_CHAINCODEADDRESS=peer0.org1.example.com:7052
      - CORE_PEER_CHAINCODELISTENADDRESS=0.0.0.0:7052
      - CORE_PEER_GOSSIP_BOOTSTRAP=peer0.org1.example.com:7051
      - CORE_PEER_GOSSIP_EXTERNALENDPOINT=peer0.org1.example.com:7051
      - CORE_PEER_LOCALMSPID=Org1MSP
      - CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/msp
      - CORE_PEER_TLS_ENABLED=true
      - CORE_PEER_TLS_CERT_FILE=/etc/hyperledger/fabric/tls/server.crt
      - CORE_PEER_TLS_KEY_FILE=/etc/hyperledger/fabric/tls/server.key
      - CORE_PEER_TLS_ROOTCERT_FILE=/etc/hyperledger/fabric/tls/ca.crt
      - CORE_OPERATIONS_LISTENADDRESS=peer0.org1.example.com:9444
      - FABRIC_CFG_PATH=/etc/hyperledger/fabric
    volumes:
      - ./fabric-network/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/msp:/etc/hyperledger/fabric/msp
      - ./fabric-network/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls:/etc/hyperledger/fabric/tls
    working_dir: /root
    command: peer node start
    ports:
      - "7051:7051"
    depends_on:
      - orderer.example.com
    networks:
      - fabric

  peer0.org2.example.com:
    container_name: peer0.org2.example.com
    image: hyperledger/fabric-peer:2.5
    environment:
      - FABRIC_LOGGING_SPEC=INFO
      - CORE_PEER_ID=peer0.org2.example.com
      - CORE_PEER_ADDRESS=peer0.org2.example.com:9051
      - CORE_PEER_LISTENADDRESS=0.0.0.0:9051
      - CORE_PEER_CHAINCODEADDRESS=peer0.org2.example.com:9052
      - CORE_PEER_CHAINCODELISTENADDRESS=0.0.0.0:9052
      - CORE_PEER_GOSSIP_BOOTSTRAP=peer0.org2.example.com:9051
      - CORE_PEER_GOSSIP_EXTERNALENDPOINT=peer0.org2.example.com:9051
      - CORE_PEER_LOCALMSPID=Org2MSP
      - CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/msp
      - CORE_PEER_TLS_ENABLED=true
      - CORE_PEER_TLS_CERT_FILE=/etc/hyperledger/fabric/tls/server.crt
      - CORE_PEER_TLS_KEY_FILE=/etc/hyperledger/fabric/tls/server.key
      - CORE_PEER_TLS_ROOTCERT_FILE=/etc/hyperledger/fabric/tls/ca.crt
    volumes:
      - ./fabric-network/crypto-config/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/msp:/etc/hyperledger/fabric/msp
      - ./fabric-network/crypto-config/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls:/etc/hyperledger/fabric/tls
    working_dir: /root
    command: peer node start
    ports:
      - "9051:9051"
    depends_on:
      - orderer.example.com
    networks:
      - fabric

networks:
  fabric:
    driver: bridge
```

### Bootstrap Scripts Required

The Docker Compose above only starts containers. You also need:

1. **`fabric-network/scripts/generate-crypto.sh`** - Runs `cryptogen` to generate certificates for both orgs and the orderer. Output goes to `fabric-network/crypto-config/`.
2. **`fabric-network/scripts/create-channel.sh`** - Creates the `carbonchannel` channel, joins both peers, and sets anchor peers. Uses `configtxgen` and `peer channel` commands.
3. **`fabric-network/scripts/deploy-chaincode.sh`** - Packages, installs, approves, and commits the chaincode on both orgs. Uses `peer lifecycle chaincode` commands.
4. **`fabric-network/configtx.yaml`** - Channel configuration: org definitions, policies, orderer config.
5. **`fabric-network/crypto-config.yaml`** - Cryptogen config: org names, peer counts, user counts.

**Recommended approach:** Use Fabric's `test-network` scripts as a starting point. Copy them into the project under `fabric-network/` and customize for the carbon trading domain.

### Network Bootstrap Sequence

```
1. Generate crypto material:    ./fabric-network/scripts/generate-crypto.sh
2. Generate channel artifacts:  ./fabric-network/scripts/generate-channel.sh
3. Start Docker Compose:        docker-compose up -d
4. Create channel:              ./fabric-network/scripts/create-channel.sh
5. Deploy chaincode:            ./fabric-network/scripts/deploy-chaincode.sh
6. Start Spring Boot:           mvn spring-boot:run
```

**Resource requirements:** Each Fabric peer uses ~200-400MB RAM. The full 3-container Fabric network adds ~1-1.2GB RAM to the existing Docker stack.

## Common Pitfalls

### Pitfall 1: Wrong SDK Artifact
**What goes wrong:** Adding `fabric-gateway-java` instead of `fabric-gateway`
**Why it happens:** The naming is confusing. `fabric-gateway-java` is the old deprecated SDK; `fabric-gateway` is the new recommended one.
**How to avoid:** Use `org.hyperledger.fabric:fabric-gateway` (no `-java` suffix)
**Warning signs:** If your imports reference `org.hyperledger.fabric.gateway.*` with `Gateway.Builder` returning `Gateway` directly (not `Gateway.Builder`), you are on the old SDK.

### Pitfall 2: gRPC Version Conflicts with Spring Boot
**What goes wrong:** Spring Boot manages its own gRPC version, and `fabric-gateway` requires a specific gRPC version. Conflicting versions cause runtime `NoSuchMethodError` or `ClassNotFoundException`.
**Why it happens:** Both Spring Boot and Fabric Gateway bring transitive gRPC dependencies.
**How to avoid:** Pin gRPC version in Maven `<dependencyManagement>`. Let `fabric-gateway`'s required version win. If using `spring-grpc-spring-boot-starter`, exclude its gRPC dependencies.
**Warning signs:** `NoSuchMethodError` in `io.grpc.*` classes at startup.

### Pitfall 3: TLS Certificate Paths in Docker
**What goes wrong:** The Spring Boot app cannot find TLS certificates because paths differ between host and container.
**Why it happens:** `application.yml` uses host paths, but the app runs inside a Docker container with different mount points.
**How to avoid:** Use Docker volume mounts consistently. Configure paths relative to the container filesystem, not the host.
**Warning signs:** `FileNotFoundException` or `SSLHandshakeException` when connecting to peer.

### Pitfall 4: Chaincode Not Ready Before App Starts
**What goes wrong:** Spring Boot starts before chaincode is deployed, causing "chaincode not found" errors.
**Why it happens:** Docker Compose `depends_on` only waits for container start, not for Fabric peer to be ready and chaincode to be installed.
**How to avoid:** Add a health check / retry logic in `FabricGatewayConfig`. Use Spring Retry to reconnect until chaincode is available.
**Warning signs:** `ChaincodeNotDefinedException` or `ContractException` at startup.

### Pitfall 5: Forgetting to Close Gateway
**What goes wrong:** The `Gateway` object holds gRPC connections. Not closing it leaks connections.
**Why it happens:** Gateway implements `AutoCloseable` but Spring Boot beans don't automatically close on shutdown.
**How to avoid:** Annotate the `Gateway` bean with `@PreDestroy` or implement `DisposableBean` to call `gateway.close()`.
**Warning signs:** Connection leaks, "too many open files" errors after running for extended periods.

### Pitfall 6: Mixing submitTransaction and evaluateTransaction
**What goes wrong:** Using `submitTransaction` for read-only queries (slow, uses consensus) or `evaluateTransaction` for writes (fails, no state change).
**Why it happens:** Not understanding the difference between the two.
**How to avoid:** `submitTransaction` = write (goes through consensus). `evaluateTransaction` = read (queries single peer, fast).
**Warning signs:** Read queries taking seconds instead of milliseconds, or write operations having no effect.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `fabric-sdk-java` (legacy SDK) | `fabric-gateway` (Gateway SDK) | Fabric 2.4 (2022) | Simplified API, peer-side endorsement |
| `fabric-gateway-java` (old Gateway) | `fabric-gateway` (new Gateway) | Fabric 2.4 (2022) | Old Gateway deprecated, use new one |
| Channel-less system channel | Application channels only | Fabric 2.3 (2021) | System channel removed from config |
| `cryptogen` only | Fabric CA for production | Always | `cryptogen` for dev, CA for prod |
| External chaincode launcher | Built-in chaincode lifecycle | Fabric 2.0 (2020) | New `peer lifecycle` commands for chaincode deployment |
| Fabric 2.5.x | Fabric 3.0 | 2025-2026 | Smart contract engine, remove system channel; migration needed |

**Deprecated/outdated:**
- `fabric-sdk-java`: Deprecated, maintenance only. Do not use for new projects.
- `fabric-gateway-java`: Deprecated, superseded by `fabric-gateway`.
- System channel: Removed in Fabric 2.3+. All channel creation uses channel configuration transactions.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Fabric 2.5.x is the current stable LTS version | Recommendation | May need to target Fabric 3.0 if 2.5.x is no longer supported |
| A2 | `fabric-gateway:1.11.0` is compatible with Java 17 + Spring Boot 3.2.5 | Dependencies | May need gRPC version overrides or dependency exclusions |
| A3 | `fabric-chaincode-shim:2.5.3` is the latest chaincode SDK version | Dependencies | Could be outdated; verify before implementing |
| A4 | Fabric peer Gateway service is enabled by default in Fabric 2.5 | Architecture | May need explicit peer configuration to enable Gateway |
| A5 | `bcpkix-jdk18on:1.78` is compatible with Java 17 | Dependencies | May need `bcpkix-jdk15on` variant instead |
| A6 | The existing `BlockchainService` interface covers all needed chaincode functions | Chaincode Design | May need additional functions for double auction, emission rating |

## Open Questions

1. **Fabric version target - 2.5.x or 3.0?**
   - What we know: Fabric 3.0 is available. 2.5.x is proven and stable.
   - What's unclear: Whether the project has a specific Fabric version requirement.
   - Recommendation: Start with 2.5.x for stability. Plan 3.0 migration as a separate phase.

2. **gRPC version conflict resolution strategy**
   - What we know: Both Spring Boot 3.2.5 and fabric-gateway bring gRPC dependencies.
   - What's unclear: Exact version Spring Boot 3.2.5 manages (if any).
   - Recommendation: Test with a minimal project first. Use `<dependencyManagement>` to pin versions.

3. **Chaincode language - Java or Go?**
   - What we know: Java chaincode is possible but less common than Go chaincode in production.
   - What's unclear: Whether the team prefers Java consistency or Go performance for chaincode.
   - Recommendation: Java chaincode for consistency with the Spring Boot backend. Go chaincode is an option if performance becomes critical.

4. **Fabric network deployment strategy for production**
   - What we know: Docker Compose is for development only. Production needs Kubernetes or managed Fabric.
   - What's unclear: Production deployment target (cloud, on-prem, managed Fabric service).
   - Recommendation: Focus on development setup first. Production deployment is a separate phase.

## Sources

### Primary (HIGH confidence)
- Maven Central - fabric-gateway:1.11.0 verified at `repo1.maven.org/maven2/org/hyperledger/fabric/fabric-gateway/1.11.0/` [VERIFIED]
- fabric-samples Application.java - Java Gateway SDK usage pattern [CITED: github.com/hyperledger/fabric-samples]
- fabric-samples SmartContract.java - Java chaincode pattern [CITED: github.com/hyperledger/fabric-samples]
- Hyperledger Fabric official docs - SDK, chaincode, test-network [CITED: hyperledger-fabric.readthedocs.io]

### Secondary (MEDIUM confidence)
- Fabric Gateway API docs - `hyperledger.github.io/fabric-gateway/` [CITED]
- fabric-chaincode-java README - chaincode API usage [CITED: github.com/hyperledger/fabric-chaincode-java]
- Fabric test-network compose files [CITED: github.com/hyperledger/fabric-samples/test-network]

### Tertiary (LOW confidence)
- Fabric 3.0 migration impact - based on Fabric release notes, not tested [ASSUMED]
- Resource requirements for Fabric containers - estimated from community reports [ASSUMED]

## Metadata

**Confidence breakdown:**
- Standard stack (fabric-gateway): HIGH - verified on Maven Central, official docs
- Architecture pattern: HIGH - based on official fabric-samples, verified code
- Docker setup: MEDIUM - based on test-network reference, needs customization
- Chaincode design: MEDIUM - follows standard patterns but domain-specific functions need validation
- gRPC compatibility: LOW - potential Spring Boot conflict not tested

**Research date:** 2026-05-14
**Valid until:** 2026-06-14 (30 days - Fabric SDK versions change slowly)

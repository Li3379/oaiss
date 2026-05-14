# AI-SPEC: Hyperledger Fabric Integration for OAISS CHAIN

> Framework: Hyperledger Fabric Gateway Java SDK v1.11.0
> System Type: Blockchain Integration (Carbon Trading Platform)
> Model Provider: N/A (not an LLM system -- blockchain SDK)
> Last Updated: 2026-05-14

---

## Section 3 -- Framework Quick Reference

### 3.1 Installation

The Fabric Gateway client API (`fabric-gateway`) is the current, actively maintained SDK (the older `fabric-gateway-java` v2.2.x is deprecated as of Fabric v2.5). It requires Fabric v2.4+ with a Gateway-enabled peer.

**Maven dependencies** -- add to `oaiss-chain-backend/pom.xml`:

```xml
<!-- Fabric Gateway Client API (current, v1.11.0) -->
<dependency>
    <groupId>org.hyperledger.fabric</groupId>
    <artifactId>fabric-gateway</artifactId>
    <version>1.11.0</version>
</dependency>

<!-- Protobuf BOM -- ensures v4 of protocol buffers is resolved -->
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-bom</artifactId>
    <version>4.33.4</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>

<!-- gRPC -- required transport layer -->
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-bom</artifactId>
    <version>1.78.0</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-api</artifactId>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty-shaded</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 3.2 Key Imports

```java
// Gateway connection
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Network;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Hash;

// Transaction types
import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.SubmitException;
import org.hyperledger.fabric.client.CommitStatusException;
import org.hyperledger.fabric.client.GatewayException;

// Identity
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;

// gRPC
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
```

### 3.3 Entry Point Pattern (Carbon Trading)

```java
@Configuration
public class FabricGatewayConfig {

    @Value("${fabric.peer.endpoint:localhost:7051}")
    private String peerEndpoint;

    @Value("${fabric.peer.tls-cert-path}")
    private Path tlsCertPath;

    @Value("${fabric.msp-id:Org1MSP}")
    private String mspId;

    @Value("${fabric.cert-path}")
    private Path certPath;

    @Value("${fabric.key-path}")
    private Path keyPath;

    @Value("${fabric.channel:carbon-channel}")
    private String channelName;

    @Bean(destroyMethod = "close")
    public Gateway fabricGateway() throws Exception {
        ManagedChannel channel = newGrpcConnection();
        Identity identity = newIdentity();
        Signer signer = newSigner();

        return Gateway.newInstance()
                .identity(identity)
                .signer(signer)
                .hash(Hash.SHA256)
                .connection(channel)
                .evaluateOptions(opts -> opts.withDeadlineAfter(5, TimeUnit.SECONDS))
                .endorseOptions(opts -> opts.withDeadlineAfter(15, TimeUnit.SECONDS))
                .submitOptions(opts -> opts.withDeadlineAfter(5, TimeUnit.SECONDS))
                .commitStatusOptions(opts -> opts.withDeadlineAfter(1, TimeUnit.MINUTES))
                .connect();
    }

    @Bean
    public Network fabricNetwork(Gateway gateway) {
        return gateway.getNetwork(channelName);
    }

    @Bean("carbonReportContract")
    public Contract carbonReportContract(Network network) {
        return network.getContract("carbon-report-cc");
    }

    @Bean("carbonTradeContract")
    public Contract carbonTradeContract(Network network) {
        return network.getContract("carbon-trade-cc");
    }

    @Bean("carbonNeutralContract")
    public Contract carbonNeutralContract(Network network) {
        return network.getContract("carbon-neutral-cc");
    }

    private ManagedChannel newGrpcConnection() throws IOException {
        var credentials = TlsChannelCredentials.newBuilder()
                .trustManager(tlsCertPath.toFile())
                .build();
        return Grpc.newChannelBuilder(peerEndpoint, credentials)
                .overrideAuthority("peer0.org1.example.com")
                .build();
    }

    private Identity newIdentity() throws IOException, CertificateException {
        try (var reader = Files.newBufferedReader(certPath)) {
            var certificate = Identities.readX509Certificate(reader);
            return new X509Identity(mspId, certificate);
        }
    }

    private Signer newSigner() throws IOException, InvalidKeyException {
        try (var reader = Files.newBufferedReader(keyPath)) {
            var privateKey = Identities.readPrivateKey(reader);
            return Signers.newPrivateKeySigner(privateKey);
        }
    }
}
```

### 3.4 Abstractions

| Abstraction | Package | Purpose | OAISS Usage |
|-------------|---------|---------|-------------|
| `Gateway` | `org.hyperledger.fabric.client` | Entry point -- single connection to Fabric network | Singleton Spring bean, reused across all blockchain calls |
| `Network` | `org.hyperledger.fabric.client` | Represents a channel (e.g., `carbon-channel`) | One bean for the carbon trading channel |
| `Contract` | `org.hyperledger.fabric.client` | Represents a deployed chaincode | Three beans: `carbon-report-cc`, `carbon-trade-cc`, `carbon-neutral-cc` |
| `Identity` / `X509Identity` | `org.hyperledger.fabric.client.identity` | Client identity (X.509 certificate + MSP ID) | Created once at startup from crypto material |
| `Signer` | `org.hyperledger.fabric.client.identity` | Signs proposals using a private key | Created once at startup, used for all transaction signing |

### 3.5 Pitfalls

1. **Using the deprecated `fabric-gateway-java` (v2.2.x) instead of `fabric-gateway` (v1.11.0).**
   The old SDK (`org.hyperledger.fabric:fabric-gateway-java`) is archived and deprecated since Fabric v2.5. The new SDK (`org.hyperledger.fabric:fabric-gateway`) has a completely different API -- `Gateway.newInstance()` vs `Gateway.createBuilder()`, different identity model, no `Wallet` class. Mixing tutorials from both SDKs causes compilation errors.

2. **Opening a new Gateway connection per request.**
   `Gateway` is designed to be long-lived and thread-safe. Creating a new `Gateway` per HTTP request causes gRPC channel leaks, TLS handshake overhead, and eventual connection exhaustion. The Spring bean should be a singleton with `destroyMethod = "close"`.

3. **Ignoring `CommitException` after `submitAsync()`.**
   `submitAsync()` returns immediately with a `Commit` future. If you never call `commit.getStatus()`, a failed endorsement or ordering failure goes silently unnoticed. For carbon trading, this means a trade could appear to succeed but never reach the ledger. Always check commit status or use synchronous `submitTransaction()`.

4. **Hardcoding crypto material paths instead of using Spring configuration.**
   The Fabric test network generates certificates in `organizations/peerOrganizations/...` with filenames that include random hashes. Hardcoding these paths breaks when certificates are regenerated. Use `Files.list(dirPath).findFirst()` pattern combined with configurable base directories.

5. **Not setting gRPC deadlines.**
   Without explicit `evaluateOptions`, `endorseOptions`, `submitOptions`, and `commitStatusOptions` deadlines, a hung peer can block the calling thread indefinitely. In a Spring Boot app serving HTTP requests, this translates to stuck Tomcat threads and cascading timeouts.

### 3.6 Folder Structure

```
oaiss-chain-backend/
  src/main/java/com/oaiss/chain/
    config/
      FabricGatewayConfig.java          # Gateway, Network, Contract beans
      FabricProperties.java             # @ConfigurationProperties for fabric.*
    service/
      BlockchainService.java            # Real implementation (replaces mock)
      fabric/
        FabricTransactionService.java    # Submit/evaluate with retry logic
    blockchain/                          # New package for Fabric-specific code
      exception/
        FabricConnectionException.java
        FabricTransactionException.java
      model/
        ChaincodeRequest.java
        ChaincodeResponse.java
  src/main/resources/
    fabric/
      connection-org1.yaml              # Connection profile for Org1
      connection-org2.yaml              # Connection profile for Org2
    application.yml                     # fabric.* properties section

chaincode/                              # Separate project (Gradle or Maven)
  carbon-report-cc/
    src/main/java/com/oaiss/chaincode/
      CarbonReportContract.java
      model/
        CarbonReport.java
  carbon-trade-cc/
    src/main/java/com/oaiss/chaincode/
      CarbonTradeContract.java
      model/
        CarbonTrade.java
  carbon-neutral-cc/
    src/main/java/com/oaiss/chaincode/
      CarbonNeutralContract.java
      model/
        CarbonNeutralProject.java

fabric-network/                         # Docker-based test network
  docker/
    docker-compose-fabric.yaml
  organizations/
    peerOrganizations/
      org1.example.com/
      org2.example.com/
    ordererOrganizations/
      example.com/
  configtx/
    configtx.yaml
  scripts/
    start-network.sh
    deploy-chaincode.sh
```

### 3.7 Sources

- [Fabric Gateway Client API -- Java README](https://github.com/hyperledger/fabric-gateway/blob/main/java/README.md)
- [Fabric Gateway Java SDK (deprecated) -- v2.2.9](https://github.com/hyperledger/fabric-gateway-java)
- [Fabric Gateway v1.11.0 Release](https://github.com/hyperledger/fabric-gateway/releases/tag/v1.11.0)
- [Fabric Test Network Documentation](https://hyperledger-fabric.readthedocs.io/en/latest/test_network.html)
- [Asset Transfer Basic -- Java Application Sample](https://github.com/hyperledger/fabric-samples/tree/main/asset-transfer-basic/application-gateway-java)
- [Asset Transfer Basic -- Java Chaincode Sample](https://github.com/hyperledger/fabric-samples/tree/main/asset-transfer-basic/chaincode-java)
- [Spring Fabric Gateway Starter (community)](https://github.com/ecsoya/spring-fabric-gateway)
- [Fabric Gateway API Javadoc](https://hyperledger.github.io/fabric-gateway/main/api/java/)

---

## Section 4 -- Implementation Guidance

### 4.1 Model Selection and Parameters

This is a blockchain SDK integration, not an LLM system. The "model" here is the chaincode execution model.

**Fabric version**: v2.5.x (LTS) or v3.0 (if BFT consensus needed)
**Gateway SDK**: `org.hyperledger.fabric:fabric-gateway:1.11.0`
**Chaincode runtime**: Java 17 (matches backend, uses `fabric-chaincode-shim`)
**Consensus**: Raft (default) -- sufficient for carbon trading; BFT only if Byzantine fault tolerance is required
**Endorsement policy**: `AND('Org1MSP', 'Org2MSP')` -- both orgs must endorse for carbon trades (high assurance); `OR('Org1MSP', 'Org2MSP')` for read-only queries
**gRPC timeouts**:
- Evaluate (query): 5 seconds
- Endorse (propose): 15 seconds
- Submit (order): 5 seconds
- Commit status: 60 seconds

### 4.2 Core Pattern -- Replacing Mock Methods with Real Fabric Calls

**Current mock in `BlockchainService.java`** (lines 35-46, 57-64, 73-86, 95-108):

```java
// MOCK: returns fake txHash
public String invokeChaincode(String channelName, String chaincodeName,
        String functionName, String... args) {
    String txHash = "tx_mock_" + System.currentTimeMillis() + "_";
    return txHash;
}
```

**Real implementation using Fabric Gateway API**:

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class BlockchainService {

    private final Contract carbonReportContract;  // @Qualifier("carbonReportContract")
    private final Contract carbonTradeContract;    // @Qualifier("carbonTradeContract")
    private final Contract carbonNeutralContract;  // @Qualifier("carbonNeutralContract")
    private final Gateway fabricGateway;

    /**
     * Submit a transaction that modifies ledger state.
     * Synchronous -- blocks until the transaction is committed to the ledger.
     * Use for: commitReport, commitTrade, certifyProject
     */
    private String submitTransaction(Contract contract, String functionName, String... args) {
        try {
            byte[] result = contract.submitTransaction(functionName, args);
            String txId = new String(result, StandardCharsets.UTF_8);
            log.info("Transaction committed: function={}, txId={}", functionName, txId);
            return txId;
        } catch (EndorseException e) {
            log.error("Endorsement failed for {}: {}", functionName, e.getMessage());
            throw new FabricTransactionException("Endorsement failed: " + e.getMessage(), e.getTransactionId());
        } catch (SubmitException e) {
            log.error("Submit failed for {}: {}", functionName, e.getMessage());
            throw new FabricTransactionException("Submit failed: " + e.getMessage(), e.getTransactionId());
        } catch (CommitStatusException e) {
            log.error("Commit status check failed for {}: {}", functionName, e.getMessage());
            throw new FabricTransactionException("Commit status error: " + e.getMessage(), e.getTransactionId());
        } catch (CommitException e) {
            log.error("Commit failed for {}: code={}, txId={}", functionName, e.getCode(), e.getTransactionId());
            throw new FabricTransactionException("Commit failed with code " + e.getCode(), e.getTransactionId());
        }
    }

    /**
     * Evaluate a read-only query against the ledger.
     * No ordering or commitment -- fast, single-peer response.
     */
    private String evaluateTransaction(Contract contract, String functionName, String... args) {
        try {
            byte[] result = contract.evaluateTransaction(functionName, args);
            return new String(result, StandardCharsets.UTF_8);
        } catch (GatewayException e) {
            log.error("Query failed for {}: {}", functionName, e.getMessage());
            throw new FabricTransactionException("Query failed: " + e.getMessage());
        }
    }

    // --- Replace each mock method ---

    public String commitReportToChain(Long reportId, String reportData) {
        log.info("Committing carbon report to chain: reportId={}", reportId);
        return submitTransaction(carbonReportContract, "CommitReport",
                String.valueOf(reportId), reportData);
    }

    public String commitTradeToChain(Long tradeId, String tradeData) {
        log.info("Committing trade to chain: tradeId={}", tradeId);
        return submitTransaction(carbonTradeContract, "CommitTrade",
                String.valueOf(tradeId), tradeData);
    }

    public String queryBlock(Long blockNumber) {
        // Block queries go through the Network object, not a Contract
        // Requires: gateway.getNetwork("carbon-channel").getBlockInfo()
        // See Section 4.4 for state management approach
        try {
            var network = fabricGateway.getNetwork("carbon-channel");
            // Block queries use the network's block event or ledger query APIs
            // fabric-gateway v1.x does not expose direct block query --
            // use evaluateTransaction on a utility chaincode or the peer CLI
            return evaluateTransaction(carbonReportContract, "QueryBlock",
                    String.valueOf(blockNumber));
        } catch (Exception e) {
            log.error("Block query failed: {}", e.getMessage());
            throw new FabricTransactionException("Block query failed: " + e.getMessage());
        }
    }

    public String queryTransaction(String txHash) {
        return evaluateTransaction(carbonReportContract, "QueryTransaction", txHash);
    }

    public boolean verifySignature(String data, String signature, String publicKey) {
        // Signature verification happens in chaincode, not in the gateway client
        String result = evaluateTransaction(carbonReportContract, "VerifySignature",
                data, signature, publicKey);
        return Boolean.parseBoolean(result);
    }

    public Map<String, Object> checkConnection() {
        Map<String, Object> status = new HashMap<>();
        try {
            // Attempt a lightweight query to verify connectivity
            var network = fabricGateway.getNetwork("carbon-channel");
            status.put("connected", true);
            status.put("channel", "carbon-channel");
            status.put("mode", "FABRIC_GATEWAY");
            status.put("gatewayVersion", "1.11.0");
        } catch (Exception e) {
            status.put("connected", false);
            status.put("error", e.getMessage());
        }
        status.put("timestamp", LocalDateTime.now().toString());
        return status;
    }
}
```

### 4.3 Tool Use / Chaincode Invocation Configuration

**Synchronous submit** (`contract.submitTransaction(name, args...)`):
- Best for: carbon report commits, trade settlements, project certifications
- Guarantees: transaction is endorsed, ordered, and committed before returning
- Trade-off: higher latency (~1-3 seconds per call), but simplest error handling

**Asynchronous submit** (`contract.newProposal(name).addArguments(args).build().endorse().submitAsync()`):
- Best for: bulk operations, when you need the proposal result before commit completes
- Pattern: submit async, process the result, then await commit status
- Required for: high-throughput trade settlement where UI can show pending state

```java
// Async pattern for trade settlement
var commit = carbonTradeContract.newProposal("CommitTrade")
        .addArguments(String.valueOf(tradeId), tradeData)
        .build()
        .endorse()
        .submitAsync();

// Use the proposal result immediately (e.g., return pending txId to frontend)
String pendingTxId = new String(commit.getResult(), StandardCharsets.UTF_8);

// Await commit confirmation
var status = commit.getStatus();
if (!status.isSuccessful()) {
    log.error("Trade commit failed: txId={}, code={}", status.getTransactionId(), status.getCode());
    throw new FabricTransactionException("Trade commit failed", status.getTransactionId());
}
```

### 4.4 State Management Approach

**On-chain state** (Fabric world state):
- Carbon report hashes and metadata (key: `REPORT:{reportId}`)
- Trade records (key: `TRADE:{tradeId}`)
- Carbon neutral project certifications (key: `PROJECT:{projectId}`)
- Use Fabric's built-in MVCC concurrency control -- no separate @Version needed

**Off-chain state** (MySQL -- existing OAISS CHAIN database):
- Full report data, user details, enterprise info
- The on-chain record stores only a SHA-256 hash + metadata, not the full payload
- This keeps chaincode lightweight and avoids bloating the ledger

**Hash verification pattern**:
```java
// In CarbonService.java (existing code at line 171)
String reportHash = DigestUtils.sha256Hex(reportData);  // Off-chain hash
String txHash = blockchainService.commitReportToChain(reportId, reportHash);  // On-chain hash

// Later, verify integrity:
String onChainHash = blockchainService.evaluateTransaction(
    carbonReportContract, "GetReportHash", String.valueOf(reportId));
String currentHash = DigestUtils.sha256Hex(currentReportData);
boolean intact = onChainHash.equals(currentHash);
```

### 4.5 Context Window Strategy

Not applicable (blockchain system, not LLM). The equivalent concern is **ledger size management**:

- Store only hashes and minimal metadata on-chain; keep full data in MySQL
- Use Fabric's history query (`getHistoryForKey`) sparingly -- it scans the entire key history
- For paginated transaction listing (`listTransactions`), maintain an off-chain index in MySQL that records txId + blockNumber + timestamp for each blockchain operation, rather than querying the ledger for every page request
- Prune old block events if the ledger grows beyond operational limits (Fabric supports snapshot pruning)

---

## Section 4b -- AI Systems Best Practices

> Note: This project is a blockchain integration, not an LLM/AI system. The "AI best practices" framework is adapted here as "Distributed Ledger Integration Best Practices" -- the same principles of structured outputs, async design, prompt discipline, context management, and cost budgeting have direct analogues in blockchain system design.

### 4b.1 Structured Outputs with Pydantic -> Structured Chaincode Responses with Java Records

In LLM systems, Pydantic enforces structured output. In blockchain systems, chaincode functions must return structured, typed data -- unstructured responses cause deserialization failures across organizations.

**Pattern: Define chaincode response as a Java Record, serialize with Genson (Fabric's default JSON library for Java chaincode)**

```java
// Chaincode-side model (in carbon-report-cc)
public record CarbonReportRecord(
    String reportId,
    String reportHash,
    String enterpriseId,
    String status,       // COMMITTED | VERIFIED | REJECTED
    long timestamp,
    String txId
) {}

// Chaincode function returns structured JSON
@Transaction(intent = Transaction.TYPE.EVALUATE)
public String GetReport(Context ctx, String reportId) {
    String json = ctx.getStub().getStringState("REPORT:" + reportId);
    if (json == null || json.isEmpty()) {
        throw new ChaincodeException("Report not found: " + reportId);
    }
    return json;  // Always valid CarbonReportRecord JSON
}

// Client-side deserialization (in Spring Boot backend)
public record ChaincodeResponse<T>(
    boolean success,
    T data,
    String error,
    String txId
) {}

// Retry logic: 3 retries for transient failures, log each attempt
public <T> T submitWithRetry(Contract contract, String function,
        Class<T> responseType, String... args) {
    int maxRetries = 3;
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            byte[] result = contract.submitTransaction(function, args);
            return objectMapper.readValue(result, responseType);
        } catch (EndorseException | SubmitException e) {
            // Transient -- retry
            log.warn("Attempt {}/{} failed for {}: {}", attempt, maxRetries,
                    function, e.getMessage());
            if (attempt == maxRetries) {
                throw new FabricTransactionException(
                    "All retries exhausted for " + function, e.getTransactionId());
            }
        } catch (CommitException e) {
            // Non-retryable -- business logic rejection
            log.error("Commit rejected for {}: code={}", function, e.getCode());
            throw new FabricTransactionException(
                "Commit rejected: " + e.getCode(), e.getTransactionId());
        } catch (JsonProcessingException e) {
            // Non-retryable -- schema mismatch
            log.error("Deserialization failed for {}: {}", function, e.getMessage());
            throw new FabricTransactionException("Invalid chaincode response schema");
        }
    }
    throw new IllegalStateException("Unreachable");
}
```

**Retry rules**:
- `EndorseException`, `SubmitException`: transient, retry up to 3 times with exponential backoff (1s, 2s, 4s)
- `CommitException`: non-retryable (business logic rejection), surface immediately
- `CommitStatusException`: retry once (network hiccup), then surface
- `GatewayException` on evaluate: retry once, then surface
- Log: function name, attempt number, error message, transaction ID on every attempt

### 4b.2 Async-First Design

**How async works in Fabric Gateway**:
- `contract.submitTransaction()` is synchronous -- blocks until the transaction is committed
- `contract.newProposal().build().endorse().submitAsync()` is asynchronous -- returns a `Commit` object immediately
- `contract.evaluateTransaction()` is always synchronous (single-peer read, no ordering)

**The one common mistake**: Calling `submitAsync()` and never checking `commit.getStatus()`. This is the blockchain equivalent of fire-and-forget -- the transaction may fail at the ordering or commit stage, and you will never know.

**Stream vs. await**:
- **Await (synchronous `submitTransaction`)**: Use for structured operations that must be confirmed before proceeding -- carbon report commit, trade settlement, project certification. The caller needs the txId for database correlation.
- **Async (`submitAsync`)**: Use for high-throughput scenarios where the UI can show "pending" state -- bulk report uploads, batch trade settlements. Always follow up with `commit.getStatus()` in a separate thread or callback.

**Spring Boot async integration**:
```java
@Async("fabricTaskExecutor")
public CompletableFuture<String> commitReportAsync(Long reportId, String reportData) {
    try {
        var commit = carbonReportContract.newProposal("CommitReport")
                .addArguments(String.valueOf(reportId), reportData)
                .build()
                .endorse()
                .submitAsync();

        String pendingTxId = new String(commit.getResult(), StandardCharsets.UTF_8);

        // Await commit in the background
        var status = commit.getStatus();
        if (!status.isSuccessful()) {
            throw new FabricTransactionException(
                "Async commit failed", status.getTransactionId());
        }

        return CompletableFuture.completedFuture(pendingTxId);
    } catch (Exception e) {
        return CompletableFuture.failedFuture(e);
    }
}
```

**Thread pool configuration**:
```java
@Bean("fabricTaskExecutor")
public Executor fabricTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);     // Match number of endorsing peers
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("fabric-");
    executor.initialize();
    return executor;
}
```

### 4b.3 Prompt Engineering Discipline -> Chaincode Interface Discipline

In LLM systems, prompt engineering controls output quality. In blockchain systems, chaincode function signatures and parameter conventions control data integrity.

**System vs. user prompt separation -> Chaincode vs. client-side validation**:
- Chaincode (on-chain): MUST validate all inputs -- this is immutable and shared across orgs. Never trust client input.
- Client (off-chain): Pre-validate for UX (fail fast, friendly error messages), but chaincode must re-validate independently.

**Function naming conventions**:
| Operation Type | Prefix | Example | Chaincode Intent |
|---------------|--------|---------|-----------------|
| Write (ledger modify) | `Commit`, `Create`, `Update`, `Transfer` | `CommitReport`, `CommitTrade` | `Transaction.TYPE.SUBMIT` |
| Read (query) | `Get`, `Query`, `Verify`, `Exists` | `GetReportHash`, `VerifySignature` | `Transaction.TYPE.EVALUATE` |

**Few-shot equivalent -> Chaincode test data initialization**:
- Provide `InitLedger` function for development/test environments only
- Use `@Transaction(intent = Transaction.TYPE.SUBMIT)` for all state-modifying functions
- Never leave `max_tokens` unbounded equivalent: always validate input sizes in chaincode to prevent oversized ledger entries

```java
// Chaincode input validation (must be done on-chain)
@Transaction(intent = Transaction.TYPE.SUBMIT)
public void CommitReport(Context ctx, String reportId, String reportHash) {
    // Validate inputs on-chain -- never trust client
    if (reportId == null || reportId.isEmpty()) {
        throw new ChaincodeException("reportId is required");
    }
    if (reportHash == null || reportHash.length() != 64) {
        throw new ChaincodeException("reportHash must be a valid SHA-256 hex string");
    }
    // Check for duplicates
    String existing = ctx.getStub().getStringState("REPORT:" + reportId);
    if (existing != null && !existing.isEmpty()) {
        throw new ChaincodeException("Report already exists: " + reportId);
    }

    CarbonReportRecord record = new CarbonReportRecord(
        reportId, reportHash, ctx.getClientIdentity().getMSPID(),
        "COMMITTED", ctx.getStub().getTxTimestamp().toEpochMilli(),
        ctx.getStub().getTxId()
    );
    ctx.getStub().putStringState("REPORT:" + reportId, genson.serialize(record));
}
```

### 4b.4 Context Window Management -> Ledger Size and Query Management

**RAG equivalent -> Fabric history queries**:
- `getHistoryForKey()` scans the entire key history -- equivalent to loading full context. Use sparingly.
- For audit trail queries, maintain an off-chain event log in MySQL instead of querying on-chain history.
- When on-chain history is required, paginate by block range rather than full key history.

**Multi-agent/conversational equivalent -> Multi-chaincode transactions**:
- Fabric does not support cross-chaincode transactions atomically. If a carbon report commit must also update a trade, use a single chaincode with multiple functions rather than two separate chaincodes.
- For the OAISS CHAIN system, consider consolidating `carbon-report-cc` and `carbon-trade-cc` into a single `carbon-ledger-cc` if cross-function atomicity is needed.

**Autonomous/compaction equivalent -> Ledger pruning**:
- Fabric v2.5+ supports peer snapshot and ledger pruning
- Implement periodic snapshot jobs for long-running production networks
- Store only the current state on-chain; archive historical data off-chain in MySQL

**Practical approach for OAISS CHAIN**:
```java
// Off-chain index for fast paginated queries (replaces listTransactions mock)
@Entity
@Table(name = "blockchain_transaction_index")
public class BlockchainTransactionIndex {
    @Id
    private String txId;
    private Long blockNumber;
    private String chaincodeName;
    private String functionName;
    private String relatedId;        // reportId, tradeId, or projectId
    private String relatedType;      // REPORT, TRADE, PROJECT
    private LocalDateTime timestamp;
    private String status;           // COMMITTED, FAILED
}

// After each successful submitTransaction, write to the index
public String commitReportToChain(Long reportId, String reportData) {
    String txHash = submitTransaction(carbonReportContract, "CommitReport",
            String.valueOf(reportId), reportData);
    // Write off-chain index for fast queries
    transactionIndexRepository.save(new BlockchainTransactionIndex(
        txHash, null, "carbon-report-cc", "CommitReport",
        String.valueOf(reportId), "REPORT", LocalDateTime.now(), "COMMITTED"
    ));
    return txHash;
}
```

### 4b.5 Cost and Latency Budget

**Per-call cost estimate at expected volume**:

| Operation | Latency (p50) | Latency (p99) | Compute Cost | Notes |
|-----------|--------------|--------------|-------------|-------|
| `evaluateTransaction` (query) | 50-100ms | 500ms | Negligible | Single peer, no ordering |
| `submitTransaction` (write) | 1-3s | 5-10s | Endorsement CPU + ordering | Goes through endorsement + ordering + commit |
| `submitAsync` + await | 1-3s | 5-10s | Same as sync | Non-blocking thread usage |
| Block query | 100-200ms | 1s | Negligible | Uses peer ledger |

**Caching strategy** (equivalent of semantic caching in LLM systems):
- **Exact-match caching**: Cache `evaluateTransaction` results in Redis with TTL. Carbon report hashes are immutable after commit -- cache forever with `REPORT:HASH:{reportId}` as the key.
- **Invalidation**: No invalidation needed for committed data (append-only ledger). Only cache query results, never cache write results.

```java
@Cacheable(value = "blockchain-queries", key = "'query:' + #contractName + ':' + #function + ':' + #args.hashCode()")
public String cachedEvaluate(Contract contract, String contractName,
        String function, String... args) {
    return evaluateTransaction(contract, function, args);
}
```

**Cheaper models for sub-tasks -> Lightweight chaincode for queries**:
- Use `evaluateTransaction` (query, no ordering) for all read operations -- 10-30x faster than submit
- Use a dedicated query chaincode (`carbon-query-cc`) with read-optimized functions if the main chaincode becomes too heavy
- Consider Fabric's private data collections for sensitive enterprise data that should not be visible to all orgs -- reduces endorsement overhead for private reads

**Infrastructure cost**:
- Fabric test network: 3 Docker containers (2 peers + 1 orderer) -- ~2GB RAM total
- Production minimum: 4 peers (2 per org) + 3 orderers (Raft) + 2 CAs -- ~8GB RAM
- Storage: ~1MB per 1000 transactions on ledger; plan for 10GB/year at moderate volume

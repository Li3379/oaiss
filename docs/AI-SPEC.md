# OAISS CHAIN AI-SPEC：Hyperledger Fabric 集成规范

> 框架：Hyperledger Fabric Gateway Java SDK v1.11.0  
> 系统类型：区块链集成（碳交易平台）  
> 模型提供方：不适用（这不是 LLM 系统，而是区块链 SDK）  
> 最后更新：2026-05-14

---

## 第 3 部分：框架速查

### 3.1 安装

Fabric Gateway 客户端 API（`fabric-gateway`）是当前仍在维护的 SDK。旧版 `fabric-gateway-java` v2.2.x 自 Fabric v2.5 起已废弃。使用当前 SDK 需要 Fabric v2.4+，并要求 peer 节点启用了 Gateway。

**Maven 依赖**，添加到 `oaiss-chain-backend/pom.xml`：

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

### 3.2 关键导入

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

### 3.3 入口模式（碳交易场景）

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

### 3.4 核心抽象

| 抽象 | 包 | 作用 | OAISS 用法 |
|---|---|---|---|
| `Gateway` | `org.hyperledger.fabric.client` | 进入 Fabric 网络的统一入口 | 作为单例 Spring Bean，复用所有区块链调用 |
| `Network` | `org.hyperledger.fabric.client` | 表示一个通道，例如 `carbon-channel` | 为碳交易通道创建一个 Bean |
| `Contract` | `org.hyperledger.fabric.client` | 表示某个已部署的链码 | 当前规划为 3 个 Bean：`carbon-report-cc`、`carbon-trade-cc`、`carbon-neutral-cc` |
| `Identity` / `X509Identity` | `org.hyperledger.fabric.client.identity` | 客户端身份（X.509 证书 + MSP ID） | 启动时从加密材料中读取并创建 |
| `Signer` | `org.hyperledger.fabric.client.identity` | 使用私钥对提案进行签名 | 启动时创建，后续所有交易复用 |

### 3.5 常见坑点

1. **错误地继续使用废弃的 `fabric-gateway-java`（v2.2.x）而不是 `fabric-gateway`（v1.11.0）**  
   旧 SDK `org.hyperledger.fabric:fabric-gateway-java` 已在 Fabric v2.5 后归档废弃。新 SDK `org.hyperledger.fabric:fabric-gateway` API 完全不同，例如 `Gateway.newInstance()` 替代了 `Gateway.createBuilder()`，身份模型也变化了，而且不再有 `Wallet`。如果混用两套教程，极容易直接编译失败。

2. **每个请求都重新打开一个 Gateway 连接**  
   `Gateway` 设计目标是长生命周期且线程安全。若每个 HTTP 请求都新建一个 `Gateway`，会导致 gRPC 通道泄漏、TLS 握手额外开销，以及最终的连接资源耗尽。Spring Bean 应保持单例，并配置 `destroyMethod = "close"`。

3. **调用 `submitAsync()` 后忽略 `CommitException` / `commit.getStatus()`**  
   `submitAsync()` 会立刻返回一个 `Commit` future。如果从不调用 `commit.getStatus()`，那么 endorsement 或 ordering 失败可能会被静默吞掉。对碳交易系统而言，这意味着交易在界面上看起来成功，但实际上并未落链。必须显式检查 commit 状态，或者直接使用同步 `submitTransaction()`。

4. **把证书、私钥路径写死在代码里，而不是走 Spring 配置**  
   Fabric 测试网络生成的证书路径中常带有随机哈希。硬编码路径会在证书重建后立即失效。应采用 `Files.list(dirPath).findFirst()` 这类模式，并结合可配置目录。

5. **未设置 gRPC deadline**  
   如果没有为 `evaluateOptions`、`endorseOptions`、`submitOptions`、`commitStatusOptions` 设置超时，卡住的 peer 会无限阻塞调用线程。在 Spring Boot 的 HTTP 服务中，这会直接变成卡死的 Tomcat 线程与级联超时。

### 3.6 目录结构建议

```text
oaiss-chain-backend/
  src/main/java/com/oaiss/chain/
    config/
      FabricGatewayConfig.java          # Gateway / Network / Contract Bean
      FabricProperties.java             # fabric.* 的 @ConfigurationProperties
    service/
      BlockchainService.java            # 真实实现（替换 mock）
      fabric/
        FabricTransactionService.java   # 带重试逻辑的 submit/evaluate
    blockchain/
      exception/
        FabricConnectionException.java
        FabricTransactionException.java
      model/
        ChaincodeRequest.java
        ChaincodeResponse.java
  src/main/resources/
    fabric/
      connection-org1.yaml              # Org1 的连接配置
      connection-org2.yaml              # Org2 的连接配置
    application.yml                     # fabric.* 配置段

chaincode/
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

fabric-network/
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

### 3.7 参考来源

- [Fabric Gateway Client API -- Java README](https://github.com/hyperledger/fabric-gateway/blob/main/java/README.md)
- [Fabric Gateway Java SDK (deprecated) -- v2.2.9](https://github.com/hyperledger/fabric-gateway-java)
- [Fabric Gateway v1.11.0 Release](https://github.com/hyperledger/fabric-gateway/releases/tag/v1.11.0)
- [Fabric Test Network Documentation](https://hyperledger-fabric.readthedocs.io/en/latest/test_network.html)
- [Asset Transfer Basic -- Java Application Sample](https://github.com/hyperledger/fabric-samples/tree/main/asset-transfer-basic/application-gateway-java)
- [Asset Transfer Basic -- Java Chaincode Sample](https://github.com/hyperledger/fabric-samples/tree/main/asset-transfer-basic/chaincode-java)
- [Spring Fabric Gateway Starter (community)](https://github.com/ecsoya/spring-fabric-gateway)
- [Fabric Gateway API Javadoc](https://hyperledger.github.io/fabric-gateway/main/api/java/)

---

## 第 4 部分：实现指导

### 4.1 模型选择与参数

这里不是 LLM 集成，而是区块链 SDK 集成。所谓“模型”，对应的是链码执行模型。

- **Fabric 版本**：`v2.5.x (LTS)`，或在确有 BFT 共识需求时使用 `v3.0`
- **Gateway SDK**：`org.hyperledger.fabric:fabric-gateway:1.11.0`
- **Chaincode 运行时**：Java 17（与后端一致，使用 `fabric-chaincode-shim`）
- **共识**：默认使用 Raft，足以支撑碳交易；只有在确需拜占庭容错时才考虑 BFT
- **背书策略**：
  - 碳交易写操作建议：`AND('Org1MSP', 'Org2MSP')`
  - 只读查询可放宽为：`OR('Org1MSP', 'Org2MSP')`
- **gRPC 超时建议**：
  - Evaluate（查询）：5 秒
  - Endorse（提案背书）：15 秒
  - Submit（排序提交）：5 秒
  - Commit status：60 秒

### 4.2 核心替换模式：将 Mock 方法替换为真实 Fabric 调用

**当前 `BlockchainService.java` 中的 mock 形式**（35-46、57-64、73-86、95-108 行附近）：

```java
// MOCK: returns fake txHash
public String invokeChaincode(String channelName, String chaincodeName,
        String functionName, String... args) {
    String txHash = "tx_mock_" + System.currentTimeMillis() + "_";
    return txHash;
}
```

**使用 Fabric Gateway API 的真实实现建议：**

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

### 4.3 链码调用方式配置

**同步提交**：`contract.submitTransaction(name, args...)`

- 适用场景：
  - 碳报告提交上链
  - 交易结算
  - 项目认证
- 保证：
  - 方法返回前，交易已经完成背书、排序并提交到账本
- 代价：
  - 单次调用延迟更高，通常约 1-3 秒
  - 但错误处理最直接、最稳妥

**异步提交**：`contract.newProposal(name).addArguments(args).build().endorse().submitAsync()`

- 适用场景：
  - 批量操作
  - 需要先拿到 proposal 结果，再等待 commit 完成
  - 高吞吐交易场景，希望前端先展示“处理中”

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

### 4.4 状态管理策略

**链上状态（Fabric world state）**

- 碳报告哈希与元数据，键建议为：`REPORT:{reportId}`
- 交易记录，键建议为：`TRADE:{tradeId}`
- 碳中和项目认证记录，键建议为：`PROJECT:{projectId}`
- 直接依赖 Fabric 内建的 MVCC 并发控制，无需再加独立 `@Version`

**链下状态（MySQL，沿用现有 OAISS CHAIN 数据库）**

- 完整报表内容、用户详情、企业信息
- 链上只保留 SHA-256 哈希与少量元数据，不保存完整业务载荷
- 这样可保持链码轻量，避免账本膨胀

**哈希校验模式：**

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

### 4.5 “上下文窗口”在区块链系统中的对应问题

这里不是 LLM 系统，因此不存在 prompt context window 的问题；它在区块链场景下对应的是 **账本规模管理**：

- 链上只存哈希和少量元数据，完整数据放在 MySQL
- 谨慎使用 `getHistoryForKey()`，因为它会扫描整个 key 的完整历史
- 对分页交易列表，不应每次都直接查账本，而是维护链下索引表，例如记录 txId、blockNumber、timestamp
- 如果账本增长到运维压力过大，应使用 Fabric 支持的 snapshot pruning 策略

---

## 第 4b 部分：把“AI 系统最佳实践”映射到 Fabric 集成

> 注意：本项目本质是区块链集成，并不是 LLM/AI 系统。这里借用“AI 系统最佳实践”的结构，只是为了把结构化输出、异步设计、接口纪律、上下文管理、成本预算这些思想映射到分布式账本场景中。

### 4b.1 从 Pydantic 结构化输出，到 Java Record 结构化链码响应

在 LLM 系统里，Pydantic 用于约束结构化输出。对应到区块链系统，链码函数同样必须返回结构化、可反序列化的类型，否则跨组织调用时很容易出错。

**建议模式：使用 Java Record 定义链码响应，并通过 Genson（Fabric Java 链码默认 JSON 库）序列化。**

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

**重试规则建议：**

- `EndorseException`、`SubmitException`：视为瞬时错误，可重试 3 次，采用指数退避（1s、2s、4s）
- `CommitException`：属于业务拒绝，不应重试，应立即上抛
- `CommitStatusException`：可在网络抖动场景下重试 1 次
- `GatewayException`（evaluate 场景）：建议重试 1 次
- 每次尝试都要记录：
  - function name
  - attempt number
  - error message
  - transaction ID

### 4b.2 异步优先设计

**Fabric Gateway 中异步的真实含义：**

- `contract.submitTransaction()`：同步，直到交易提交到账本才返回
- `contract.newProposal().build().endorse().submitAsync()`：异步，立刻返回一个 `Commit` 对象
- `contract.evaluateTransaction()`：永远是同步查询，不涉及排序

**最常见错误**：调用 `submitAsync()` 后，从不检查 `commit.getStatus()`。这在区块链里就等价于“fire-and-forget”，交易可能在排序或提交阶段失败，但调用方永远不知道。

**什么时候 await，什么时候 async：**

- **同步提交**：适合必须确认成功后才能继续的操作，例如碳报告上链、交易结算、项目认证
- **异步提交**：适合高吞吐批量场景，例如批量报告上传、批量结算。前端可以先显示“处理中”，后台再等待 commit 完成

**Spring Boot 异步集成示例：**

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

**线程池配置建议：**

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

### 4b.3 从 Prompt Discipline 映射到 Chaincode Interface Discipline

LLM 系统里，prompt 工程控制输出质量；区块链系统里，链码函数签名与参数约定控制数据一致性。

**“system vs user prompt 分离”在区块链里的对应关系：**

- 链码侧（链上）：**必须** 对所有输入做校验，这是不可篡改的共享规则，绝不能信任客户端
- 客户端（链下）：可以先做 UX 友好的预校验，但链码仍必须独立再次校验

**函数命名约定：**

| 操作类型 | 前缀 | 示例 | 链码事务意图 |
|---|---|---|---|
| 写操作（修改账本） | `Commit`、`Create`、`Update`、`Transfer` | `CommitReport`、`CommitTrade` | `Transaction.TYPE.SUBMIT` |
| 读操作（查询） | `Get`、`Query`、`Verify`、`Exists` | `GetReportHash`、`VerifySignature` | `Transaction.TYPE.EVALUATE` |

**few-shot 对应物：测试数据初始化**

- 可以在开发/测试网络中提供 `InitLedger`
- 所有写操作都明确使用 `@Transaction(intent = Transaction.TYPE.SUBMIT)`
- 类似“不要让 max_tokens 无上限”的原则，对应到链码里就是：必须限制输入大小，防止账本条目过大

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

### 4b.4 从 Context Management 映射到账本规模与查询管理

**RAG 在 Fabric 中的对应物：历史查询**

- `getHistoryForKey()` 会扫描该 key 的完整历史，代价高，应谨慎使用
- 对审计轨迹需求，优先在 MySQL 中维护链下事件日志，而不是每次都直接扫链上历史
- 如确需链上历史，建议按 block range 分段，而不是一次性读取全量历史

**多 agent / 会话协作的对应物：跨链码事务**

- Fabric 不支持多个链码之间天然原子事务
- 如果“碳报告提交”必须同时更新“交易状态”，更稳妥的是用单一链码内多个函数完成，而不是拆成两个独立链码
- 对 OAISS CHAIN 而言，如果未来需要跨功能强一致性，可以考虑将 `carbon-report-cc` 与 `carbon-trade-cc` 合并为 `carbon-ledger-cc`

**自动压缩 / 长上下文裁剪的对应物：账本修剪**

- Fabric v2.5+ 支持 peer snapshot 与 ledger pruning
- 长期运行的生产网络应配套周期性 snapshot 任务
- 链上保留当前状态，历史明细归档到 MySQL

**OAISS CHAIN 的实际建议：**

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

### 4b.5 成本与延迟预算

**在预期业务量下的单次调用成本估算：**

| 操作 | 延迟（p50） | 延迟（p99） | 计算成本 | 说明 |
|---|---|---|---|---|
| `evaluateTransaction`（查询） | 50-100ms | 500ms | 很低 | 单 peer 查询，无排序 |
| `submitTransaction`（写入） | 1-3s | 5-10s | 背书 CPU + 排序成本 | 需经历背书、排序、提交 |
| `submitAsync` + await | 1-3s | 5-10s | 与同步相同 | 只是线程占用方式不同 |
| 区块查询 | 100-200ms | 1s | 很低 | 走 peer 账本查询 |

**缓存策略**，对应于 LLM 场景中的 semantic caching：

- **精确命中缓存**：将 `evaluateTransaction` 结果缓存到 Redis，并设置 TTL。对于一经提交就不可变的碳报告哈希，理论上可永久缓存，例如键名 `REPORT:HASH:{reportId}`
- **失效策略**：对已提交数据通常无需失效，只缓存查询结果，不缓存写入结果

```java
@Cacheable(value = "blockchain-queries", key = "'query:' + #contractName + ':' + #function + ':' + #args.hashCode()")
public String cachedEvaluate(Contract contract, String contractName,
        String function, String... args) {
    return evaluateTransaction(contract, function, args);
}
```

**“更便宜模型做子任务”的区块链类比：轻量查询链码**

- 所有读操作尽量用 `evaluateTransaction`，通常比 submit 快 10-30 倍
- 如果主链码越来越重，可考虑单独做读优化链码，例如 `carbon-query-cc`
- 如某些企业数据不应被所有组织看到，可考虑使用 Fabric private data collections，既保护隐私，也减少不必要背书

**基础设施成本估算：**

- Fabric 测试网络：3 个 Docker 容器（2 个 peer + 1 个 orderer），约 2GB RAM
- 生产最小规模：4 个 peer（每个组织 2 个）+ 3 个 orderer（Raft）+ 2 个 CA，约 8GB RAM
- 存储：账本大约每 1000 笔交易增加约 1MB，按中等业务量估算，每年预留约 10GB

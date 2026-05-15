# Phase 9: 区块链真实对接 - Research

**Researched:** 2026-05-15
**Domain:** Hyperledger Fabric Gateway SDK integration with Spring Boot 3.2
**Confidence:** MEDIUM

## Summary

Phase 9 upgrades the OAISS CHAIN blockchain layer from a mock implementation that returns fake transaction hashes to a real Hyperledger Fabric network. The current `BlockchainService` contains 6 public methods that generate UUID-based fake data. The replacement requires: (1) a Fabric Gateway SDK client connecting to a gRPC peer endpoint, (2) a minimal Fabric test network running in Docker alongside existing MySQL/Redis/MinIO containers, (3) a chaincode smart contract for carbon report/trade recording, and (4) optional Fabric CA integration for blockchain identity certificates.

The Fabric Gateway SDK for Java (`org.hyperledger.fabric:fabric-gateway:1.7.1`) is the recommended client library. It uses gRPC to connect to a Fabric peer, provides `Gateway.newInstance()` builder pattern, and offers `contract.submitTransaction()` (ledger write) and `contract.evaluateTransaction()` (ledger read) APIs. The SDK requires X.509 certificates and private keys for identity -- stored as files or loaded from classpath resources.

The key risk is infrastructure complexity: a minimal Fabric network requires 4 Docker containers (peer, orderer, CA, CouchDB), plus chaincode deployment steps. The recommended mitigation is a phased approach: first get the Gateway SDK connecting to a fabric-samples test-network, then extract the Docker configuration into the project's docker-compose.yml.

**Primary recommendation:** Use Fabric Gateway SDK 1.7.1 with a Go chaincode deployed on a Fabric 2.5.x test network. Keep the existing `BlockchainService` interface intact and replace the mock internals with real SDK calls behind a `@Profile("fabric")` / `@Profile("mock-blockchain")` toggle.

## User Constraints (from CONTEXT.md)

No CONTEXT.md found for this phase. Research is unconstrained.

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| REQ-05 | Hyperledger Fabric 真实对接 -- BlockchainService 从 Mock 升级为 Fabric Gateway SDK | Current Mock analysis (Section 1), Fabric Gateway SDK (Section 2), Migration strategy (Section 7) |
| REQ-12 | 身份认证区块链方案 -- Fabric CA 集成 (optional, 可降级为 mock CA) | Fabric CA integration (Section 6) |

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Fabric Gateway connection | API / Backend | -- | Backend owns the gRPC client to Fabric peer |
| Chaincode execution (submit/eval) | API / Backend | -- | Transaction submission goes through Backend service |
| Fabric network (peer, orderer, CA) | Docker Infrastructure | -- | External service containers, not application code |
| Chaincode logic (store, query, history) | Fabric Network (chaincode) | API / Backend | Chaincode runs on peer; Backend invokes it |
| Fabric CA certificate issuance | Fabric Network (CA) | API / Backend | CA runs in Docker; Backend calls CA API to enroll users |
| Blockchain data display | Browser / Client | API / Backend | Frontend shows blockchain data via Backend API (no direct chain access) |
| Profile-based Mock/Fabric toggle | API / Backend | -- | Spring `@Profile` mechanism for switching implementations |

## 1. Current Mock Implementation Analysis

### BlockchainService.java -- Mock Patterns to Replace

The current `BlockchainService` is a `@Service` with 6 public methods that all return fake data:

| Method | Current Mock Behavior | Real Fabric Replacement |
|--------|----------------------|------------------------|
| `commitReportToChain(reportId, reportHash, enterpriseId, enterpriseName)` | Generates UUID as txHash, sets current time as timestamp, returns fake `BlockchainResponse` | `contract.submitTransaction("CreateCarbonReport", reportId, reportHash, enterpriseId, enterpriseName)` |
| `queryTransaction(txHash)` | Creates fake `BlockchainTransactionResponse` with random blockNumber (1-99999) | `contract.evaluateTransaction("QueryTransaction", txHash)` |
| `queryBlock(blockNumber)` | Creates fake `BlockchainBlockResponse` with random transactions | `contract.evaluateTransaction("QueryBlock", String.valueOf(blockNumber))` |
| `queryReportHistory(reportId)` | Creates fake list of 1-5 history entries | `contract.evaluateTransaction("GetReportHistory", reportId)` |
| `commitTradeToChain(tradeId, ...)` | Same UUID-based mock as report | `contract.submitTransaction("CreateTradeRecord", tradeId, ...)` |
| `verifyReportOnChain(reportId, reportHash)` | Always returns `true` | `contract.evaluateTransaction("VerifyReport", reportId, reportHash)` |

### Data Flow -- How BlockchainService is Used

```
CarbonService.approveReport()
  -> blockchainService.commitReportToChain(reportId, reportHash, enterpriseId, enterpriseName)
  -> carbonReport.setBlockchainTxHash(response.getTxHash())
  -> carbonReport.setOnChainAt(LocalDateTime.now())
  -> save to MySQL

BlockchainController (6 endpoints)
  -> /api/v1/blockchain/transaction/{txHash}  -> queryTransaction()
  -> /api/v1/blockchain/block/{blockNumber}   -> queryBlock()
  -> /api/v1/blockchain/report/{reportId}     -> queryReportHistory()
  -> /api/v1/blockchain/verify/{reportId}     -> verifyReportOnChain()
  -> /api/v1/blockchain/stats                 -> getBlockchainStats() (hardcoded mock)
  -> /api/v1/blockchain/enterprise/{id}/stats -> getEnterpriseStats() (hardcoded mock)
```

### Key Observations

1. **No BlockchainRecord entity exists** -- Blockchain data is only stored as `blockchainTxHash` and `onChainAt` fields on `CarbonReport`. The Fabric ledger itself becomes the source of truth for blockchain data.
2. **BlockchainException is a RuntimeException** -- already exists and is handled by GlobalExceptionHandler. No change needed.
3. **Error codes already defined** -- `ErrorCode` has BLOCKCHAIN_TX_COMMIT_FAILED (50001), BLOCKCHAIN_TX_QUERY_FAILED (50002), BLOCKCHAIN_BLOCK_QUERY_FAILED (50003), BLOCKCHAIN_CHANNEL_JOIN_FAILED (50004), BLOCKCHAIN_IDENTITY_AUTH_FAILED (50005), BLOCKCHAIN_SMART_CONTRACT_ERROR (50006).
4. **Frontend expects same API shape** -- `Blockchain.vue` calls `/api/v1/blockchain/*` endpoints and displays `BlockchainTransactionResponse`, `BlockchainBlockResponse` DTOs. The response DTO structure must remain identical.

### DTOs That Must Be Preserved

- `BlockchainResponse` (txHash, blockNumber, timestamp, channelId, status)
- `BlockchainTransactionResponse` (txHash, blockNumber, timestamp, fromAddress, toAddress, data, status, channelId)
- `BlockchainBlockResponse` (blockNumber, blockHash, previousHash, timestamp, transactionCount, transactions)
- `BlockchainStatsResponse` (totalTransactions, totalBlocks, activeChannels, peerCount)
- `BlockchainReportHistoryResponse` (reportId, entries: List<HistoryEntry>)

## 2. Fabric Gateway SDK for Java

### Recommended SDK Version

**`org.hyperledger.fabric:fabric-gateway:1.7.1`** [VERIFIED: Maven Central]

This is the new Fabric Gateway SDK (not the legacy `fabric-gateway-java:2.2.x`). The new SDK:
- Requires Fabric 2.4+ peer with Gateway service enabled
- Uses gRPC to connect to a single peer endpoint
- Simplified API compared to legacy SDK
- Actively maintained by Hyperledger

### Maven Dependencies

```xml
<!-- Fabric Gateway SDK - core client -->
<dependency>
    <groupId>org.hyperledger.fabric</groupId>
    <artifactId>fabric-gateway</artifactId>
    <version>1.7.1</version>
</dependency>

<!-- gRPC dependencies (required by Fabric Gateway) -->
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty-shaded</artifactId>
    <version>1.62.2</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-protobuf</artifactId>
    <version>1.62.2</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-stub</artifactId>
    <version>1.62.2</version>
</dependency>

<!-- Bouncy Castle for certificate handling -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcpkix-jdk18on</artifactId>
    <version>1.78.1</version>
</dependency>
```

### API Pattern -- Connection, Submit, Evaluate

Source: fabric-samples `asset-transfer-basic/application-gateway-java/src/main/java/App.java` [VERIFIED: GitHub hyperledger/fabric-samples]

```java
// 1. Create gRPC channel to peer
private static ManagedChannel newGrpcChannel() throws IOException {
    var tlsCert = Files.readAllBytes(Paths.get("crypto/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt"));
    var tlsRootCert = Identities.readX509Certificate(new String(tlsCert));
    var tlsBuilder = NettyChannelBuilder.forTarget("peer0.org1.example.com:7051")
        .sslContext(GrpcSslContexts.configure(SslContextBuilder.forClient(), GrpcSslContexts.forClient())
            .trustManager(tlsRootCert)
            .build())
        .build();
    return tlsBuilder;
}

// 2. Create identity from certificate
private static Identity newIdentity() throws IOException, CertificateException {
    var cert = Files.readAllBytes(Paths.get("crypto/peerOrganizations/org1.example.com/users/User1@org1.example.com/certificates/User1@org1.example.com-cert.pem"));
    var certificate = Identities.readX509Certificate(new String(cert));
    return new X509Identity("Org1MSP", certificate);
}

// 3. Create signer from private key
private static Signer newSigner() throws IOException, InvalidKeyException {
    var keyBytes = Files.readAllBytes(Paths.get("crypto/peerOrganizations/org1.example.com/users/User1@org1.example.com/private-key/key.pem"));
    var privateKey = Identities.readPrivateKey(new String(keyBytes));
    return Signers.newPrivateKeySigner(privateKey);
}

// 4. Build Gateway
var gateway = Gateway.newInstance()
    .identity(identity)
    .signer(signer)
    .connection(channel)
    .connect();

// 5. Get network and contract
var network = gateway.getNetwork("mychannel");
var contract = network.getContract("carbon-chaincode");

// 6. Submit transaction (writes to ledger, goes through orderer consensus)
byte[] result = contract.submitTransaction("CreateCarbonReport", reportId, reportHash, enterpriseId, enterpriseName);

// 7. Evaluate transaction (reads from ledger, local peer only, fast)
byte[] result = contract.evaluateTransaction("QueryReportHistory", reportId);
```

### Key API Concepts

| Concept | Class/Method | Purpose |
|---------|-------------|---------|
| Gateway connection | `Gateway.newInstance().identity().signer().connection().connect()` | Establish connection to Fabric peer |
| Network | `gateway.getNetwork("channel-name")` | Access a specific channel |
| Contract | `network.getContract("chaincode-name")` | Access a specific smart contract |
| Submit | `contract.submitTransaction("func", args...)` | Write to ledger (requires consensus) |
| Evaluate | `contract.evaluateTransaction("func", args...)` | Read from ledger (local peer only) |
| Identity | `new X509Identity(mspId, certificate)` | X.509 certificate identity |
| Signer | `Signers.newPrivateKeySigner(privateKey)` | Private key transaction signing |

### Identity and Certificate Management

The Gateway SDK requires:
1. **X.509 certificate** (PEM format) -- identifies the user on the Fabric network
2. **Private key** (PEM format) -- signs transactions
3. **Peer TLS CA certificate** -- validates the peer's TLS certificate
4. **MSP ID** -- organization membership (e.g., "Org1MSP")

In development (test-network), these files are generated by the `network.sh` script in `crypto/` directory.
In production, Fabric CA issues these certificates.

### Gateway Instance Lifecycle

**Critical pattern:** The Gateway instance should be created once at application startup and reused. Creating a new Gateway for each transaction is wasteful (gRPC connection setup, TLS handshake).

Recommended Spring integration:
```java
@Configuration
@Profile("fabric")
public class FabricGatewayConfig {

    @Bean(destroyMethod = "close")
    public Gateway fabricGateway(FabricProperties props) throws Exception {
        var channel = newGrpcChannel(props);
        var identity = newIdentity(props);
        var signer = newSigner(props);

        return Gateway.newInstance()
            .identity(identity)
            .signer(signer)
            .connection(channel)
            .connect();
    }

    @Bean
    public Network fabricNetwork(Gateway gateway, FabricProperties props) {
        return gateway.getNetwork(props.getChannelName());
    }

    @Bean
    public Contract carbonContract(Network network, FabricProperties props) {
        return network.getContract(props.getChaincodeName());
    }
}
```

## 3. Fabric Network Docker Setup

### Minimal Required Containers

A minimal Fabric test network requires:

| Container | Image | Port | Purpose |
|-----------|-------|------|---------|
| peer0.org1.example.com | `hyperledger/fabric-peer:2.5` | 7051 (gRPC), 9444 (operations) | Endorser + committer |
| orderer.example.com | `hyperledger/fabric-orderer:2.5` | 7050 (gRPC), 9443 (operations) | Raft consensus orderer |
| ca.org1.example.com | `hyperledger/fabric-ca:1.5` | 7054 (HTTP) | Certificate Authority for Org1 |
| couchdb0 | `couchdb:3.3` | 5984 (HTTP) | Rich query database for peer |

**Fabric version recommendation:** Use Fabric 2.5.x LTS (not 3.x). Fabric 3.x introduces breaking changes and the Gateway SDK 1.7.1 has better compatibility with 2.5.x. [ASSUMED] -- the Gateway SDK README references 2.4+ compatibility but 3.x changes are not yet fully documented.

### Docker Compose Addition Pattern

```yaml
# Add to existing docker-compose.yml
services:
  # ... existing mysql, redis, minio, backend, frontend ...

  peer0.org1.example.com:
    image: hyperledger/fabric-peer:2.5
    environment:
      - CORE_PEER_ID=peer0.org1.example.com
      - CORE_PEER_ADDRESS=peer0.org1.example.com:7051
      - CORE_PEER_LISTENADDRESS=0.0.0.0:7051
      - CORE_PEER_CHAINCODEADDRESS=peer0.org1.example.com:7052
      - CORE_PEER_CHAINCODELISTENADDRESS=0.0.0.0:7052
      - CORE_PEER_GOSSIP_BOOTSTRAP=peer0.org1.example.com:7051
      - CORE_PEER_GOSSIP_EXTERNALENDPOINT=peer0.org1.example.com:7051
      - CORE_PEER_LOCALMSPID=Org1MSP
      - CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/msp
      - CORE_OPERATIONS_ADDRESS=peer0.org1.example.com:9444
      - CORE_METRICS_PROVIDER=prometheus
      - FABRIC_LOGGING_SPEC=INFO
      - CORE_PEER_TLS_ENABLED=true
      - CORE_PEER_TLS_CERT_FILE=/etc/hyperledger/fabric/tls/server.crt
      - CORE_PEER_TLS_KEY_FILE=/etc/hyperledger/fabric/tls/server.key
      - CORE_PEER_TLS_ROOTCERT_FILE=/etc/hyperledger/fabric/tls/ca.crt
      - CORE_LEDGER_STATE_COUCHDBCONFIG_COUCHDBADDRESS=couchdb0:5984
      - CORE_LEDGER_STATE_STATEDATABASE=CouchDB
    volumes:
      - ./fabric-config/crypto/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/msp:/etc/hyperledger/fabric/msp
      - ./fabric-config/crypto/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls:/etc/hyperledger/fabric/tls
      - peer0.org1.example.com:/var/hyperledger/production
    ports:
      - "7051:7051"
    depends_on:
      - couchdb0
    networks:
      - oaiss-network

  orderer.example.com:
    image: hyperledger/fabric-orderer:2.5
    environment:
      - FABRIC_LOGGING_SPEC=INFO
      - ORDERER_GENERAL_LISTENADDRESS=0.0.0.0
      - ORDERER_GENERAL_LISTENPORT=7050
      - ORDERER_GENERAL_LOCALMSPID=OrdererMSP
      - ORDERER_GENERAL_LOCALMSPDIR=/etc/hyperledger/fabric/msp
      - ORDERER_GENERAL_TLS_ENABLED=true
      - ORDERER_GENERAL_TLS_PRIVATEKEY=/etc/hyperledger/fabric/tls/server.key
      - ORDERER_GENERAL_TLS_CERTIFICATE=/etc/hyperledger/fabric/tls/server.crt
      - ORDERER_GENERAL_TLS_ROOTCAS=[/etc/hyperledger/fabric/tls/ca.crt]
      - ORDERER_CHANNELPARTICIPATION_ENABLED=true
      - ORDERER_ADMIN_TLS_ENABLED=true
    volumes:
      - ./fabric-config/crypto/ordererOrganizations/example.com/orderers/orderer.example.com/msp:/etc/hyperledger/fabric/msp
      - ./fabric-config/crypto/ordererOrganizations/example.com/orderers/orderer.example.com/tls:/etc/hyperledger/fabric/tls
      - orderer.example.com:/var/hyperledger/production
    ports:
      - "7050:7050"
    networks:
      - oaiss-network

  ca.org1.example.com:
    image: hyperledger/fabric-ca:1.5
    environment:
      - FABRIC_CA_HOME=/etc/hyperledger/fabric-ca-server
      - FABRIC_CA_SERVER_CA_NAME=ca-org1
      - FABRIC_CA_SERVER_TLS_ENABLED=true
    volumes:
      - ./fabric-config/crypto/fabric-ca/org1:/etc/hyperledger/fabric-ca-server
    ports:
      - "7054:7054"
    networks:
      - oaiss-network

  couchdb0:
    image: couchdb:3.3
    environment:
      - COUCHDB_USER=admin
      - COUCHDB_PASSWORD=adminpw
    ports:
      - "5984:5984"
    networks:
      - oaiss-network
```

### Recommended Setup Approach

**Do NOT try to manually create crypto materials.** Instead:

1. Use `fabric-samples/test-network` to generate initial crypto materials and channel configuration
2. Copy the generated `crypto/` and `channel-artifacts/` directories into the project as `fabric-config/`
3. Extract the Docker Compose service definitions from `test-network/compose/` into the project's `docker-compose.yml`
4. This avoids the complexity of running `cryptogen`, `configtxgen`, and other Fabric CLI tools manually

**Setup commands:**
```bash
# 1. Clone fabric-samples
curl -sSLO https://raw.githubusercontent.com/hyperledger/fabric/main/scripts/install-fabric.sh && bash install-fabric.sh docker samples binary

# 2. Start test network (generates crypto + creates channel)
cd fabric-samples/test-network
./network.sh up createChannel -c mychannel -ca

# 3. Deploy chaincode
./network.sh deployCC -ccn carbon-chaincode -ccp ../../oaiss-chain-chaincode -ccl go

# 4. Copy generated crypto materials
cp -r organizations/ ../../fabric-config/crypto/
```

### Channel and Chaincode Deployment Steps

1. **Create channel** -- `./network.sh createChannel -c mychannel`
2. **Join peer to channel** -- handled by `network.sh`
3. **Package chaincode** -- `peer lifecycle chaincode package carbon-chaincode.tar.gz --path ./chaincode --lang golang --label carbon-chaincode_1.0`
4. **Install on peer** -- `peer lifecycle chaincode install carbon-chaincode.tar.gz`
5. **Approve for org** -- `peer lifecycle chaincode approveformyorg -o orderer.example.com:7050 --channelID mychannel --name carbon-chaincode --version 1.0 --sequence 1`
6. **Commit definition** -- `peer lifecycle chaincode commit -o orderer.example.com:7050 --channelID mychannel --name carbon-chaincode --version 1.0 --sequence 1`

## 4. Chaincode Design

### Language Recommendation: Go

**Use Go for chaincode, not Java.** Rationale:
- Go chaincode is the standard in Fabric documentation and samples [VERIFIED: fabric-samples]
- Go chaincode has smaller Docker image size, faster startup
- Java chaincode requires a separate JVM in the chaincode container (heavyweight)
- The project's backend is Java, but chaincode runs as an independent process on the peer -- language choice is independent
- Go chaincode tooling (`go mod`, `go test`) is simpler and well-documented for Fabric

### Minimal Chaincode Functions

```go
package main

import (
    "encoding/json"
    "fmt"
    "time"

    "github.com/hyperledger/fabric-contract-api-go/contractapi"
)

// CarbonChaincode -- smart contract for carbon report and trade recording
type CarbonChaincode struct {
    contractapi.Contract
}

// CarbonReportRecord -- data stored on chain
type CarbonReportRecord struct {
    ReportID      string `json:"reportId"`
    ReportHash    string `json:"reportHash"`
    EnterpriseID  string `json:"enterpriseId"`
    EnterpriseName string `json:"enterpriseName"`
    TxType        string `json:"txType"`     // "CARBON_REPORT"
    Timestamp     string `json:"timestamp"`
    TxHash        string `json:"txHash"`      // populated after commit
    BlockNumber   int64  `json:"blockNumber"` // populated after commit
}

// TradeRecord -- trade data stored on chain
type TradeRecord struct {
    TradeID       string `json:"tradeId"`
    SellerID      string `json:"sellerId"`
    BuyerID       string `json:"buyerId"`
    Amount        string `json:"amount"`
    Price         string `json:"price"`
    TxType        string `json:"txType"`     // "CARBON_TRADE"
    Timestamp     string `json:"timestamp"`
}

// CreateCarbonReport -- submit a carbon report to the ledger
func (cc *CarbonChaincode) CreateCarbonReport(ctx contractapi.TransactionContextInterface,
    reportId, reportHash, enterpriseId, enterpriseName string) error {

    record := CarbonReportRecord{
        ReportID:       reportId,
        ReportHash:     reportHash,
        EnterpriseID:   enterpriseId,
        EnterpriseName: enterpriseName,
        TxType:         "CARBON_REPORT",
        Timestamp:      time.Now().UTC().Format(time.RFC3339),
    }

    recordBytes, err := json.Marshal(record)
    if err != nil {
        return fmt.Errorf("failed to marshal report: %v", err)
    }

    return ctx.GetStub().PutState("REPORT_"+reportId, recordBytes)
}

// QueryReportHistory -- get report history from chain
func (cc *CarbonChaincode) QueryReportHistory(ctx contractapi.TransactionContextInterface,
    reportId string) ([]*CarbonReportRecord, error) {

    resultsIterator, err := ctx.GetStub().GetHistoryForKey("REPORT_" + reportId)
    if err != nil {
        return nil, err
    }
    defer resultsIterator.Close()

    var records []*CarbonReportRecord
    for resultsIterator.HasNext() {
        response, err := resultsIterator.Next()
        if err != nil {
            return nil, err
        }
        var record CarbonReportRecord
        if err := json.Unmarshal(response.Value, &record); err != nil {
            continue
        }
        record.TxHash = response.TxId
        records = append(records, &record)
    }
    return records, nil
}

// CreateTradeRecord -- record a carbon trade on chain
func (cc *CarbonChaincode) CreateTradeRecord(ctx contractapi.TransactionContextInterface,
    tradeId, sellerId, buyerId, amount, price string) error {

    record := TradeRecord{
        TradeID:   tradeId,
        SellerID:  sellerId,
        BuyerID:   buyerId,
        Amount:    amount,
        Price:     price,
        TxType:    "CARBON_TRADE",
        Timestamp: time.Now().UTC().Format(time.RFC3339),
    }

    recordBytes, err := json.Marshal(record)
    if err != nil {
        return fmt.Errorf("failed to marshal trade: %v", err)
    }

    return ctx.GetStub().PutState("TRADE_"+tradeId, recordBytes)
}

// VerifyReport -- verify a report exists on chain with matching hash
func (cc *CarbonChaincode) VerifyReport(ctx contractapi.TransactionContextInterface,
    reportId, expectedHash string) (bool, error) {

    recordBytes, err := ctx.GetStub().GetState("REPORT_" + reportId)
    if err != nil {
        return false, err
    }
    if recordBytes == nil {
        return false, nil
    }

    var record CarbonReportRecord
    if err := json.Unmarshal(recordBytes, &record); err != nil {
        return false, err
    }

    return record.ReportHash == expectedHash, nil
}

// GetTransactionByID -- query a specific transaction
func (cc *CarbonChaincode) GetTransactionByID(ctx contractapi.TransactionContextInterface,
    txId string) ([]byte, error) {

    // Use GetHistoryForKey or direct state query
    // This is a simplified version
    return ctx.GetStub().GetState(txId)
}

func main() {
    cc, err := contractapi.NewChaincode(&CarbonChaincode{})
    if err != nil {
        fmt.Printf("Error creating chaincode: %v", err)
        return
    }
    if err := cc.Start(); err != nil {
        fmt.Printf("Error starting chaincode: %v", err)
    }
}
```

### Chaincode Directory Structure

```
oaiss-chain-chaincode/
  go.mod                     # module definition
  go.sum                     # dependency checksums
  chaincode.go               # main entry + smart contract
  go                         # Go 1.21+
```

### Chaincode Dependencies (go.mod)

```
module github.com/oaiss/chain/chaincode

go 1.21

require (
    github.com/hyperledger/fabric-contract-api-go v1.2.2
)
```

### Chaincode Deployment

Chaincode is deployed as an external process (Fabric 2.x lifecycle):
1. Package: `peer lifecycle chaincode package carbon-chaincode.tar.gz`
2. Install: `peer lifecycle chaincode install carbon-chaincode.tar.gz`
3. Approve: `peer lifecycle chaincode approveformyorg`
4. Commit: `peer lifecycle chaincode commit`

## 5. Spring Boot Integration Design

### FabricProperties Configuration

```java
@ConfigurationProperties(prefix = "fabric")
@Data
public class FabricProperties {
    private boolean enabled = false;        // toggle fabric vs mock
    private String mspId = "Org1MSP";
    private String channelName = "mychannel";
    private String chaincodeName = "carbon-chaincode";
    private String peerEndpoint = "peer0.org1.example.com:7051";
    private String peerTlsCertPath = "classpath:fabric/crypto/peer-tls-ca.crt";
    private String certPath = "classpath:fabric/crypto/user-cert.pem";
    private String keyPath = "classpath:fabric/crypto/user-key.pem";
    private boolean tlsEnabled = true;
}
```

### application.yml Addition

```yaml
fabric:
  enabled: false                          # default: mock mode
  msp-id: Org1MSP
  channel-name: mychannel
  chaincode-name: carbon-chaincode
  peer-endpoint: peer0.org1.example.com:7051
  tls-enabled: true
  peer-tls-cert-path: classpath:fabric/crypto/peer-tls-ca.crt
  cert-path: classpath:fabric/crypto/user-cert.pem
  key-path: classpath:fabric/crypto/user-key.pem
```

### FabricBlockchainService Implementation Pattern

```java
@Service
@Profile("fabric")
@RequiredArgsConstructor
@Slf4j
public class FabricBlockchainService implements BlockchainServicePort {

    private final Contract carbonContract;
    private final FabricProperties props;

    @Override
    public BlockchainResponse commitReportToChain(String reportId, String reportHash,
                                                   String enterpriseId, String enterpriseName) {
        try {
            byte[] result = carbonContract.submitTransaction(
                "CreateCarbonReport", reportId, reportHash, enterpriseId, enterpriseName);
            // Parse result to extract txHash, blockNumber
            return parseSubmitResult(result);
        } catch (Exception e) {
            throw new BlockchainException(ErrorCode.BLOCKCHAIN_TX_COMMIT_FAILED,
                "Failed to commit report to Fabric: " + e.getMessage());
        }
    }

    @Override
    public BlockchainTransactionResponse queryTransaction(String txHash) {
        try {
            byte[] result = carbonContract.evaluateTransaction("GetTransactionByID", txHash);
            return parseTransactionResult(result);
        } catch (Exception e) {
            throw new BlockchainException(ErrorCode.BLOCKCHAIN_TX_QUERY_FAILED,
                "Failed to query transaction from Fabric: " + e.getMessage());
        }
    }
    // ... other methods
}
```

### Interface Extraction

The current `BlockchainService` is a concrete class. For the Profile-based toggle, extract an interface:

```java
public interface BlockchainServicePort {
    BlockchainResponse commitReportToChain(String reportId, String reportHash,
                                            String enterpriseId, String enterpriseName);
    BlockchainTransactionResponse queryTransaction(String txHash);
    BlockchainBlockResponse queryBlock(Long blockNumber);
    List<BlockchainReportHistoryResponse> queryReportHistory(String reportId);
    BlockchainResponse commitTradeToChain(String tradeId, String sellerId,
                                           String buyerId, String amount, String price);
    boolean verifyReportOnChain(String reportId, String reportHash);
}
```

Then:
- `MockBlockchainService implements BlockchainServicePort` with `@Profile("mock-blockchain")` (current logic)
- `FabricBlockchainService implements BlockchainServicePort` with `@Profile("fabric")` (real SDK calls)

### Transaction Result Parsing

When `submitTransaction()` returns, the SDK provides:
- `contract.submitTransaction()` returns `byte[]` from the chaincode response
- To get the actual transaction hash, use `contract.newProposal("func", args).build().send().getResult()` for lower-level access
- Transaction events are available via the committed listener pattern

**Simplified approach for MVP:** After `submitTransaction()`, query the transaction by the composite key to get the full record including the Fabric-generated transaction ID.

## 6. Fabric CA Integration (REQ-12 -- Optional)

### What Fabric CA Does

Fabric CA is a Certificate Authority for Hyperledger Fabric that:
1. **Enrolls** users -- issues X.509 certificates (enrollment certificates)
2. **Registers** users -- creates identities with attributes (role, affiliation)
3. **Issues TLS certificates** -- for secure communication
4. **Supports attribute-based access control** -- via certificate attributes

### Integration Strategy

**Recommended: Phased approach with mock CA fallback**

| Phase | CA Implementation | When |
|-------|-------------------|------|
| Phase 9 MVP | Mock CA (pre-generated certs from test-network) | Initial integration |
| Phase 9+ | Real Fabric CA with enrollment API | After basic chaincode works |

### Mock CA Approach (MVP)

For Phase 9 MVP, use pre-generated certificates from the `fabric-samples/test-network`:
1. Run `./network.sh up createChannel -ca` -- this uses Fabric CA to generate all crypto materials
2. Copy the generated user certificates into `src/main/resources/fabric/crypto/`
3. The Spring Boot app loads these static certificates at startup
4. No runtime enrollment needed -- the "mock CA" is simply static cert files

**Limitation:** Only one user identity (User1@org1.example.com) is available. All blockchain operations use this single identity.

### Real Fabric CA Approach (Post-MVP)

```java
@Service
@Profile("fabric-ca")
@RequiredArgsConstructor
public class FabricCaService {

    private final FabricProperties props;

    /**
     * Enroll a user with Fabric CA, returning X.509 certificate and private key.
     * This maps a JWT-authenticated user to a blockchain identity.
     */
    public EnrollmentResult enrollUser(String userId, String secret) {
        // Use Fabric CA Client SDK
        // POST to http://ca.org1.example.com:7054/api/v1/enroll
        // Returns signed certificate + private key
    }

    /**
     * Register a new user with Fabric CA.
     * Called when an enterprise user first needs blockchain identity.
     */
    public RegistrationResult registerUser(String userId, String role) {
        // Requires an admin enrollment first
        // POST to http://ca.org1.example.com:7054/api/v1/register
    }
}
```

### Fabric CA Java SDK

**`org.hyperledger.fabric-ca-sdk:fabric-ca-sdk:1.5.8`** [VERIFIED: Maven Central]

```xml
<dependency>
    <groupId>org.hyperledger.fabric-ca-sdk</groupId>
    <artifactId>fabric-ca-sdk</artifactId>
    <version>1.5.8</version>
</dependency>
```

### JWT-to-Fabric-Identity Mapping

The existing JWT authentication system and Fabric CA are **independent identity systems**:
- JWT handles application-level auth (username/password -> JWT token)
- Fabric CA handles blockchain-level identity (X.509 certificate on the Fabric network)

**Recommended mapping strategy:**
1. Enterprise user logs in via JWT (existing flow)
2. When the user first triggers a blockchain operation, check if a Fabric identity exists for this user
3. If not, register + enroll the user with Fabric CA (or use a shared organizational identity)
4. Cache the Fabric identity (cert + key) in Redis keyed by userId
5. For MVP, use a single organizational identity (simpler, acceptable for a carbon reporting platform)

## 7. Migration Strategy: Mock -> Fabric

### Approach: Profile-Based Toggle with Interface Extraction

```
Phase 1: Extract BlockchainServicePort interface
         -> Rename BlockchainService -> MockBlockchainService
         -> Add @Profile("mock-blockchain") to MockBlockchainService

Phase 2: Add Fabric dependencies to pom.xml
         -> Create FabricProperties, FabricGatewayConfig
         -> Create FabricBlockchainService with @Profile("fabric")

Phase 3: Set up Fabric network Docker containers
         -> Generate crypto materials via test-network
         -> Add peer, orderer, CA, CouchDB to docker-compose.yml

Phase 4: Develop and deploy chaincode
         -> Write Go chaincode (carbon-chaincode)
         -> Package, install, approve, commit on the channel

Phase 5: Integration testing
         -> docker-compose up (Fabric network starts)
         -> Spring Boot with fabric.enabled=true
         -> Verify submitTransaction and evaluateTransaction
```

### API Contract Preservation

The existing REST API endpoints and DTOs MUST NOT change. The BlockchainController continues to call `BlockchainServicePort` methods, and the Profile determines whether mock or Fabric data is returned.

| What | Change Required |
|------|----------------|
| `BlockchainController` | NO -- inject `BlockchainServicePort` instead of `BlockchainService` |
| `BlockchainResponse` DTO | NO -- same fields, populated from Fabric data instead of UUID |
| `BlockchainTransactionResponse` DTO | NO -- map Fabric transaction data to existing fields |
| `BlockchainBlockResponse` DTO | NO -- map Fabric block data to existing fields |
| `CarbonService` | NO -- still calls `BlockchainServicePort.commitReportToChain()` |
| `BlockchainException` | NO -- same exception, thrown by both implementations |
| `ErrorCode` constants | NO -- same error codes, thrown by both implementations |
| Frontend `Blockchain.vue` | NO -- no changes, same API endpoints and response shape |
| Frontend `blockchain.ts` API client | NO -- no changes |

### Post-Commit Data Retrieval

After `submitTransaction()` returns, the chaincode response contains the data written. However, to get the Fabric-generated **transaction hash** and **block number**, additional steps are needed:

```java
// Approach: Use the committed transaction result
var proposal = carbonContract.newProposal("CreateCarbonReport", reportId, reportHash, enterpriseId, enterpriseName);
var transaction = proposal.build().send();
var result = transaction.getResult();      // chaincode return value
var txId = transaction.getTransactionId(); // Fabric transaction hash

// Wait for commit to get block number
var committed = transaction.getCommitted();
var blockNumber = committed.getBlockNumber();
```

This pattern gives us the real `txHash` (transaction ID) and `blockNumber` to populate the response DTOs.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| fabric-gateway | 1.7.1 | Fabric Gateway SDK for Java | Official Hyperledger Java SDK [VERIFIED: Maven Central] |
| fabric-ca-sdk | 1.5.8 | Fabric CA client for enrollment | Official Fabric CA Java SDK [VERIFIED: Maven Central] |
| grpc-netty-shaded | 1.62.2 | gRPC transport for Fabric peer connection | Required by fabric-gateway |
| bcpkix-jdk18on | 1.78.1 | Bouncy Castle PKI for certificate handling | Required for X.509 cert parsing |
| fabric-contract-api-go | 1.2.2 | Go chaincode API | Standard Fabric chaincode framework |

### Infrastructure
| Component | Image | Version | Purpose |
|-----------|-------|---------|---------|
| fabric-peer | hyperledger/fabric-peer | 2.5 | Blockchain peer node |
| fabric-orderer | hyperledger/fabric-orderer | 2.5 | Consensus orderer |
| fabric-ca | hyperledger/fabric-ca | 1.5 | Certificate Authority |
| couchdb | couchdb | 3.3 | Rich query state database |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| grpc-protobuf | 1.62.2 | Protocol buffer support for gRPC | Always (with fabric-gateway) |
| grpc-stub | 1.62.2 | gRPC stub generation | Always (with fabric-gateway) |

**Installation:**
```xml
<!-- In oaiss-chain-backend/pom.xml -->
<dependency>
    <groupId>org.hyperledger.fabric</groupId>
    <artifactId>fabric-gateway</artifactId>
    <version>1.7.1</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty-shaded</artifactId>
    <version>1.62.2</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-protobuf</artifactId>
    <version>1.62.2</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-stub</artifactId>
    <version>1.62.2</version>
</dependency>
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcpkix-jdk18on</artifactId>
    <version>1.78.1</version>
</dependency>
```

## Architecture Patterns

### System Architecture Diagram

```
                    +---------------------------+
                    |    Frontend (Vue 3)       |
                    |  Blockchain.vue           |
                    +------------+--------------+
                                 |
                     HTTP /api/v1/blockchain/*
                                 |
                    +------------v--------------+
                    |   BlockchainController     |
                    |   (Spring REST)           |
                    +------------+--------------+
                                 |
                        BlockchainServicePort
                       (interface injection)
                      /                     \
                     /                       \
    +----------------v--+          +-----------v-----------+
    | MockBlockchain    |          | FabricBlockchain     |
    | Service           |          | Service              |
    | @Profile("mock")  |          | @Profile("fabric")   |
    +-------------------+          +-----------+-----------+
                                               |
                                    Fabric Gateway SDK
                                    (gRPC + TLS)
                                               |
                    +------------v--------------+
                    |  Fabric Peer (Docker)     |
                    |  peer0.org1.example.com   |
                    +------------+--------------+
                                 |
                    +------------v--------------+
                    |  Chaincode Container       |
                    |  carbon-chaincode (Go)     |
                    +------------+--------------+
                                 |
                    +------------v--------------+
                    |  Fabric Orderer (Docker)  |
                    |  CouchDB (Docker)         |
                    |  Fabric CA (Docker)       |
                    +---------------------------+
```

### Recommended Project Structure

```
OAISS CHAIN/
  oaiss-chain-backend/               # existing
    src/main/java/com/oaiss/chain/
      service/
        BlockchainServicePort.java    # NEW: extracted interface
        MockBlockchainService.java    # RENAMED from BlockchainService
        FabricBlockchainService.java  # NEW: real Fabric implementation
      config/
        FabricProperties.java         # NEW: @ConfigurationProperties
        FabricGatewayConfig.java      # NEW: Gateway bean setup
    src/main/resources/
      fabric/                         # NEW: crypto materials
        crypto/
          peer-tls-ca.crt
          user-cert.pem
          user-key.pem
  oaiss-chain-chaincode/              # NEW: Go chaincode project
    go.mod
    go.sum
    chaincode.go
  fabric-config/                      # NEW: Fabric network config
    crypto/                           # generated by test-network
    channel-artifacts/                # generated by test-network
    configtx.yaml                     # channel configuration
  docker-compose.yml                  # UPDATED: add Fabric containers
```

### Pattern 1: Profile-Based Implementation Toggle
**What:** Use Spring `@Profile` to switch between mock and real Fabric implementations
**When to use:** Any external service that has a mock and real variant
**Example:**
```java
// Interface
public interface BlockchainServicePort {
    BlockchainResponse commitReportToChain(String reportId, String reportHash,
                                            String enterpriseId, String enterpriseName);
}

// Mock implementation (default)
@Service
@Profile("mock-blockchain")
public class MockBlockchainService implements BlockchainServicePort { ... }

// Real implementation
@Service
@Profile("fabric")
public class FabricBlockchainService implements BlockchainServicePort { ... }
```

### Pattern 2: Gateway Singleton with Spring Lifecycle
**What:** Create Fabric Gateway as a Spring bean with proper startup/shutdown
**When to use:** Expensive resources that should be reused (gRPC connections)
**Example:**
```java
@Configuration
@Profile("fabric")
public class FabricGatewayConfig {
    @Bean(destroyMethod = "close")
    public Gateway fabricGateway(FabricProperties props) throws Exception {
        // Create and return Gateway instance
    }
}
```

### Anti-Patterns to Avoid
- **Creating a new Gateway per request:** Wasteful -- Gateway wraps a gRPC connection that should be long-lived
- **Storing private keys in application.yml:** Cryptographic material must be file-based, not embedded in config
- **Calling submitTransaction synchronously in web request:** Fabric consensus takes 1-3 seconds; consider async with CompletableFuture
- **Using fabric-sdk-java (legacy):** The old SDK is deprecated; use fabric-gateway instead
- **Using Fabric 3.x with Gateway SDK 1.7.1:** Compatibility not fully verified; stick with Fabric 2.5.x LTS

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Fabric peer connection | Custom gRPC client | `fabric-gateway` SDK | TLS, identity, signing handled by SDK |
| Certificate parsing | Custom X.509 parser | `Identities.readX509Certificate()` | Handles PEM/DER, Bouncy Castle integration |
| Transaction signing | Custom ECDSA signer | `Signers.newPrivateKeySigner()` | Correct curve, encoding, Fabric-compliant |
| Fabric CA enrollment | HTTP client to CA API | `fabric-ca-sdk` | Handles CSR generation, response parsing |
| Channel configuration | Custom protobuf | `configtxgen` + `network.sh` | Complex protobuf structures, easy to get wrong |
| Crypto material generation | Custom cert tool | `cryptogen` or Fabric CA | PKI infrastructure, root CAs, TLS certs |

**Key insight:** The Fabric SDK handles all the cryptographic and consensus complexity. The application code should only call `submitTransaction()` and `evaluateTransaction()` -- never try to construct or sign Fabric protocol messages directly.

## Common Pitfalls

### Pitfall 1: Fabric Network Startup Order
**What goes wrong:** Spring Boot starts before Fabric peer is ready; Gateway connection fails
**Why it happens:** Docker Compose `depends_on` only checks container start, not service readiness
**How to avoid:** Add health checks to Fabric containers; use Spring Boot `@ConditionalOnProperty` with `fabric.enabled=true` and retry logic in Gateway bean
**Warning signs:** Application crash on startup with "connection refused" to peer:7051

### Pitfall 2: TLS Certificate Mismatch
**What goes wrong:** gRPC TLS handshake fails because the peer TLS cert doesn't match the trusted CA cert
**Why it happens:** Peer certificate regenerated but old CA cert still in classpath resources
**How to avoid:** Keep TLS CA cert and peer cert in sync; regenerate together; use volume mounts in Docker
**Warning signs:** `SSLHandshakeException`, `CERTIFICATE_VERIFY_FAILED`

### Pitfall 3: submitTransaction Timeout
**What goes wrong:** `submitTransaction()` blocks for 30+ seconds and times out
**Why it happens:** Orderer is down, channel not created, or chaincode not deployed
**How to avoid:** Add explicit timeout configuration; check Fabric network health before submitting; use async pattern
**Warning signs:** Timeout after 30 seconds; `EndorsementFailure` errors

### Pitfall 4: Mixing Fabric SDK Versions
**What goes wrong:** Runtime classpath conflicts between `fabric-gateway` and `fabric-sdk-java`
**Why it happens:** Both libraries have overlapping transitive dependencies
**How to avoid:** Only use `fabric-gateway` (org.hyperledger.fabric:fabric-gateway); do NOT add `fabric-sdk-java` (org.hyperledger.fabric-sdk-java)
**Warning signs:** `ClassNotFoundException`, `NoSuchMethodError` at runtime

### Pitfall 5: Windows Docker Performance for Fabric
**What goes wrong:** Fabric peer and chaincode containers are extremely slow on Windows Docker Desktop
**Why it happens:** WSL2 filesystem performance for bind mounts; Fabric uses extensive file I/O
**How to avoid:** Use named volumes instead of bind mounts for peer data; increase Docker memory to 8GB+; consider running Fabric on a Linux VM
**Warning signs:** 2-3 minute chaincode instantiation; peer container frequently restarting

### Pitfall 6: Chaincode Not Found After Deploy
**What goes wrong:** `contract.evaluateTransaction()` throws "chaincode not found" even after deployment
**Why it happens:** Chaincode was installed but not committed, or committed on wrong channel
**How to avoid:** Verify with `peer lifecycle chaincode querycommitted`; ensure channel name matches exactly
**Warning signs:** `CHAINCODE_NOT_FOUND` error from evaluate/submit

## Code Examples

### Complete FabricGatewayConfig (Spring Boot)

```java
// Source: Adapted from fabric-samples/application-gateway-java/App.java
@Configuration
@Profile("fabric")
@EnableConfigurationProperties(FabricProperties.class)
@RequiredArgsConstructor
@Slf4j
public class FabricGatewayConfig {

    private final FabricProperties props;

    @Bean(destroyMethod = "close")
    public Gateway fabricGateway() throws Exception {
        log.info("Connecting to Fabric peer at {}", props.getPeerEndpoint());

        // 1. Build gRPC channel to peer
        ManagedChannel channel = newGrpcChannel();

        // 2. Load identity from certificate
        Identity identity = newIdentity();

        // 3. Load signer from private key
        Signer signer = newSigner();

        // 4. Build and connect Gateway
        return Gateway.newInstance()
            .identity(identity)
            .signer(signer)
            .connection(channel)
            .connect();
    }

    @Bean
    public Network fabricNetwork(Gateway gateway) {
        return gateway.getNetwork(props.getChannelName());
    }

    @Bean
    public Contract carbonContract(Network network) {
        return network.getContract(props.getChaincodeName());
    }

    private ManagedChannel newGrpcChannel() throws Exception {
        var tlsCertReader = new InputStreamReader(
            new ClassPathResource(props.getPeerTlsCertPath().replace("classpath:", ""))
                .getInputStream());
        var tlsRootCert = Identities.readX509Certificate(tlsCertReader);

        return NettyChannelBuilder.forTarget(props.getPeerEndpoint())
            .sslContext(GrpcSslContexts.forClient()
                .trustManager(tlsRootCert)
                .build())
            .build();
    }

    private Identity newIdentity() throws Exception {
        var certReader = new InputStreamReader(
            new ClassPathResource(props.getCertPath().replace("classpath:", ""))
                .getInputStream());
        var certificate = Identities.readX509Certificate(certReader);
        return new X509Identity(props.getMspId(), certificate);
    }

    private Signer newSigner() throws Exception {
        var keyReader = new InputStreamReader(
            new ClassPathResource(props.getKeyPath().replace("classpath:", ""))
                .getInputStream());
        var privateKey = Identities.readPrivateKey(keyReader);
        return Signers.newPrivateKeySigner(privateKey);
    }
}
```

### FabricBlockchainService commitReportToChain

```java
// Source: Adapted from Fabric Gateway SDK pattern
@Override
public BlockchainResponse commitReportToChain(String reportId, String reportHash,
                                               String enterpriseId, String enterpriseName) {
    try {
        // Use proposed transaction for full result including txId
        var proposal = carbonContract.newProposal("CreateCarbonReport",
            reportId, reportHash, enterpriseId, enterpriseName);
        var transaction = proposal.build().send();
        var txId = transaction.getTransactionId();

        // Wait for commit to get block number
        var committed = transaction.getCommitted();
        var blockNumber = committed.getBlockNumber();

        return BlockchainResponse.builder()
            .txHash(txId)
            .blockNumber(blockNumber)
            .timestamp(LocalDateTime.now())
            .channelId(props.getChannelName())
            .status("COMMITTED")
            .build();
    } catch (EndorsementException e) {
        log.error("Endorsement failed for report {}: {}", reportId, e.getMessage());
        throw new BlockchainException(ErrorCode.BLOCKCHAIN_SMART_CONTRACT_ERROR,
            "Chaincode endorsement failed: " + e.getMessage());
    } catch (CommitException e) {
        log.error("Commit failed for report {}: {}", reportId, e.getMessage());
        throw new BlockchainException(ErrorCode.BLOCKCHAIN_TX_COMMIT_FAILED,
            "Transaction commit failed: " + e.getMessage());
    } catch (Exception e) {
        log.error("Unexpected Fabric error for report {}: {}", reportId, e.getMessage());
        throw new BlockchainException(ErrorCode.BLOCKCHAIN_TX_COMMIT_FAILED,
            "Fabric operation failed: " + e.getMessage());
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| fabric-sdk-java (legacy) | fabric-gateway SDK | Fabric 2.4+ (2022) | New SDK is simpler, single-peer connection |
| Programmatic channel creation | Channel participation API | Fabric 2.3+ (2021) | No configtx.yaml for channel creation needed |
| cryptogen tool | Fabric CA | Always recommended for prod | Use cryptogen only for dev/test |
| Java chaincode | Go chaincode preferred | Community consensus | Go is lighter, faster, better documented |
| Legacy lifecycle (instantiate) | New lifecycle (approve+commit) | Fabric 2.0 (2020) | Must use new lifecycle process |

**Deprecated/outdated:**
- `fabric-sdk-java` (org.hyperledger.fabric-sdk-java): Replaced by `fabric-gateway`. Still available but not recommended for new projects.
- `fabric-gateway-java` (org.hyperledger.fabric:fabric-gateway-java): Legacy Gateway peer-side implementation. Replaced by `fabric-gateway` (org.hyperledger.fabric:fabric-gateway).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Fabric 2.5.x is compatible with Gateway SDK 1.7.1; Fabric 3.x compatibility not verified | Standard Stack | May need Fabric version adjustment; test before committing |
| A2 | Go chaincode is preferred over Java chaincode for new projects | Chaincode Design | If Java chaincode is required, project structure changes significantly |
| A3 | Single organizational identity is acceptable for MVP (no per-user Fabric identity) | Fabric CA | If per-user identity is required, Fabric CA integration becomes mandatory |
| A4 | Fabric peer can run on Windows Docker Desktop with acceptable performance | Pitfall 5 | May need Linux VM or WSL2 tuning |
| A5 | The `newProposal().build().send().getCommitted()` pattern works with SDK 1.7.1 | Code Examples | API may differ slightly; verify with SDK javadoc |
| A6 | Docker Compose can run Fabric containers alongside existing MySQL/Redis/MinIO | Docker Setup | Port conflicts or resource constraints possible |

## Open Questions

1. **Fabric network version: 2.5.x LTS vs 3.x?**
   - What we know: Gateway SDK 1.7.1 requires Fabric 2.4+. Fabric 3.1.4 is latest release.
   - What's unclear: Whether Gateway SDK 1.7.1 is fully compatible with Fabric 3.x
   - Recommendation: Use Fabric 2.5.x LTS for stability. Verify 3.x compatibility later.

2. **Transaction async vs sync: should blockchain commits be asynchronous?**
   - What we know: `submitTransaction()` takes 1-3 seconds (consensus time). Current mock returns instantly.
   - What's unclear: Whether the frontend expects synchronous responses for blockchain operations
   - Recommendation: Start synchronous (matching current behavior). Add async with CompletableFuture in a later phase if needed.

3. **How to get blockNumber from a committed transaction?**
   - What we know: The `getCommitted()` method exists on the transaction object
   - What's unclear: Exact API shape for block number retrieval in SDK 1.7.1
   - Recommendation: Verify with SDK javadoc during implementation. May need a block listener pattern.

4. **Should chaincode be in a separate Git repository?**
   - What we know: Chaincode has its own Go module, build process, and deployment lifecycle
   - What's unclear: Team preference for monorepo vs separate repo
   - Recommendation: Keep in same repo under `oaiss-chain-chaincode/` for simplicity in this phase.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker Desktop | Fabric network containers | Needs check | -- | Cannot run Fabric without Docker |
| Go 1.21+ | Chaincode compilation | Needs check | -- | Cannot build chaincode without Go |
| 8GB+ RAM | Fabric peer + orderer + CouchDB | Needs check | -- | Reduce to peer-only (no CouchDB) at 4GB |
| fabric-samples | Crypto material generation | Not installed | -- | Manual crypto generation (harder) |
| peer CLI binary | Chaincode lifecycle commands | Not installed | -- | Use Docker exec into peer container |

**Missing dependencies with no fallback:**
- Docker Desktop: Required to run Fabric containers. Must be installed and running.
- Go toolchain: Required to compile chaincode. Must be installed for `go build`.

**Missing dependencies with fallback:**
- fabric-samples: Can manually create crypto materials (complex) or copy from a pre-generated archive
- peer CLI: Can use `docker exec` into the Fabric peer container to run lifecycle commands

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito (backend) |
| Config file | pom.xml (Surefire/Failsafe plugins) |
| Quick run command | `cd oaiss-chain-backend && mvn test -pl . -Dtest=BlockchainServiceTest` |
| Full suite command | `cd oaiss-chain-backend && mvn verify` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| REQ-05 | FabricBlockchainService.commitReportToChain submits to Fabric | Unit (mock Contract) | `mvn test -Dtest=FabricBlockchainServiceTest` | Wave 0 |
| REQ-05 | FabricBlockchainService.queryTransaction evaluates on Fabric | Unit (mock Contract) | `mvn test -Dtest=FabricBlockchainServiceTest` | Wave 0 |
| REQ-05 | Profile toggle switches between mock and fabric | Unit (Spring context) | `mvn test -Dtest=BlockchainProfileTest` | Wave 0 |
| REQ-05 | FabricGatewayConfig creates Gateway bean | Unit | `mvn test -Dtest=FabricGatewayConfigTest` | Wave 0 |
| REQ-12 | FabricCaService enrolls users (optional) | Unit (mock CA) | `mvn test -Dtest=FabricCaServiceTest` | Wave 0 |
| REQ-05 | End-to-end submit + query on real Fabric | Integration | `mvn verify -Dit.class=FabricIntegrationTest` | Wave 0 |

### Sampling Rate
- **Per task commit:** `mvn test -Dtest=FabricBlockchainServiceTest`
- **Per wave merge:** `mvn verify`
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `FabricBlockchainServiceTest.java` -- unit tests for Fabric implementation (mock Contract)
- [ ] `BlockchainProfileTest.java` -- Spring profile toggle test
- [ ] `FabricGatewayConfigTest.java` -- Gateway configuration test
- [ ] `FabricCaServiceTest.java` -- Fabric CA service test (optional)
- [ ] `FabricIntegrationTest.java` -- integration test with Testcontainers Fabric
- [ ] Chaincode unit tests: `chaincode_test.go` -- Go chaincode logic tests

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | JWT auth unchanged; Fabric identity is separate |
| V3 Session Management | no | No session changes |
| V4 Access Control | yes | Existing @PreAuthorize; Fabric MSP-based access on chain |
| V5 Input Validation | yes | Existing validation; chaincode must validate inputs |
| V6 Cryptography | yes | Fabric uses X.509 certs, ECDSA signing, TLS -- all handled by SDK |

### Known Threat Patterns for Fabric Integration

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Private key exposure | Information Disclosure | Store keys in classpath resources (not in source control); use Fabric CA for enrollment |
| Transaction tampering | Tampering | Fabric consensus (endorsing, ordering, validating) prevents tampering |
| Unauthorized chaincode invocation | Elevation of Privilege | Fabric MSP identity + channel policies control who can invoke |
| TLS man-in-the-middle | Spoofing | Mutual TLS between Gateway SDK and peer |
| Chaincode injection | Tampering | Validate all string inputs in chaincode before writing to ledger |

### Key Security Considerations

1. **Private keys must NOT be committed to Git.** Add `fabric-config/crypto/` to `.gitignore`. In CI/CD, generate crypto materials dynamically.
2. **Fabric TLS is mandatory** -- never disable TLS in production. Development can use `tls-enabled: false` but only for local testing.
3. **The Gateway identity (user cert + private key) is a shared secret** -- in MVP, all backend requests use the same Fabric identity. In production, each user should have their own identity.
4. **Chaincode input validation** -- the Go chaincode must validate all string inputs (non-empty, reasonable length) before writing to the ledger.

## Sources

### Primary (HIGH confidence)
- Maven Central: `org.hyperledger.fabric:fabric-gateway:1.7.1` verified available
- Maven Central: `org.hyperledger.fabric-ca-sdk:fabric-ca-sdk:1.5.8` verified available
- GitHub hyperledger/fabric-samples: App.java Gateway pattern verified
- GitHub hyperledger/fabric-gateway: README.md API patterns verified
- GitHub hyperledger/fabric-gateway: Java source (Gateway.java, contract API) verified

### Secondary (MEDIUM confidence)
- GitHub hyperledger/fabric-samples: docker-compose-test-net.yaml (Docker setup pattern)
- GitHub hyperledger/fabric-samples: chaincode-go smart contract pattern
- GitHub hyperledger/fabric: Latest release v3.1.4

### Tertiary (LOW confidence)
- Fabric 2.5.x LTS compatibility with Gateway SDK 1.7.1 -- not explicitly verified against compatibility matrix
- `newProposal().build().send().getCommitted()` API shape -- inferred from Gateway.java source, not tested
- Fabric CA SDK enrollment API pattern -- based on library existence, not live-tested

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Maven Central versions verified, GitHub source code read
- Architecture: MEDIUM - Pattern derived from fabric-samples but not tested in this project
- Pitfalls: MEDIUM - Based on Fabric community knowledge and Docker on Windows experience
- Chaincode design: MEDIUM - Based on fabric-samples Go chaincode, adapted for carbon domain
- Fabric CA: LOW - Optional requirement, minimal research, needs implementation verification

**Research date:** 2026-05-15
**Valid until:** 2026-06-15 (30 days -- Fabric SDK versions are stable)

# OAISS CHAIN 区块链集成技术规范 (SPEC)

**版本**: 1.0.0
**日期**: 2026-05-10
**状态**: 规划中

---

## 1. 概述

### 1.1 背景

根据原始文档（03-项目详细方案.md），OAISS CHAIN 系统应基于 Hyperledger Fabric 区块链平台构建，实现：

- 企业碳核算数据的不可篡改存储
- 碳交易的去中心化执行
- 基于区块链的身份认证
- 可追溯的审计日志

### 1.2 当前状态

| 功能 | 原始需求 | 当前实现 | 状态 |
|------|----------|----------|------|
| 区块链底层 | Hyperledger Fabric | 模拟实现 | ⚠️ 差异 |
| 节点架构 | 企业/审核员/管理员/监管机构节点 | 传统服务架构 | ⚠️ 差异 |
| 智能合约 | Chaincode执行交易 | 模拟交易逻辑 | ⚠️ 差异 |
| 身份认证 | Fabric CA | JWT + Spring Security | ⚠️ 差异 |
| 数据上链 | 所有关键数据上链 | 部分模拟 | ⚠️ 差异 |

---

## 2. 目标架构

### 2.1 Fabric网络拓扑

```
┌─────────────────────────────────────────────────────────────────┐
│                     Hyperledger Fabric Network                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Orderer    │  │   Orderer    │  │   Orderer    │          │
│  │   Node 1     │  │   Node 2     │  │   Node 3     │          │
│  │  (Admin)     │  │  (Admin)     │  │  (Admin)     │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    Peer Nodes                             │   │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐     │   │
│  │  │ Peer 1  │  │ Peer 2  │  │ Peer 3  │  │ Peer 4  │     │   │
│  │  │Reviewer │  │Reviewer │  │Authenti-│  │Third    │     │   │
│  │  │ Org     │  │ Org     │  │cator Org│  │Party Org│     │   │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘     │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    Enterprise Clients                     │   │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐     │   │
│  │  │ Enter.  │  │ Enter.  │  │ Enter.  │  │ Enter.  │     │   │
│  │  │   A     │  │   B     │  │   C     │  │   D     │     │   │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘     │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 节点角色映射

| 原始文档角色 | Fabric角色 | 职责 |
|-------------|-----------|------|
| 企业用户 | Client | 提交交易请求、查询数据 |
| 数据审核员 | Endorser Peer | 验证交易、背书签名 |
| 认证员 | Endorser Peer | 签发准入证书、验证数字签名 |
| 管理员 | Orderer + Committer | 网络管理、区块排序、账本提交 |
| 第三方监管机构 | Observer Peer | 只读访问、审计监督 |

### 2.3 Channel 设计

```
Channel: carbon-channel
├── Members: 所有组织
├── Chaincode: carbon_cc
├── 账本数据:
│   ├── 碳报告记录
│   ├── 交易记录
│   ├── 信誉分变更记录
│   └── 审计日志
```

---

## 3. 智能合约规范

### 3.1 Chaincode 接口定义

```go
// carbon_chaincode.go

package main

import (
    "github.com/hyperledger/fabric-contract-api-go/contractapi"
)

// CarbonContract 碳核算与交易智能合约
type CarbonContract struct {
    contractapi.Contract
}

// ==================== 碳报告相关 ====================

// SubmitCarbonReport 提交碳报告
func (c *CarbonContract) SubmitCarbonReport(ctx contractapi.TransactionContextInterface, 
    reportID string, 
    enterpriseID string,
    emissionData string,
    signature string) error {
    // 1. 验证企业身份
    // 2. 验证数字签名
    // 3. 存储报告数据
    // 4. 发出事件通知审核员
}

// ApproveCarbonReport 审核通过碳报告
func (c *CarbonContract) ApproveCarbonReport(ctx contractapi.TransactionContextInterface,
    reportID string,
    reviewerID string,
    approvalSignature string) error {
    // 1. 验证审核员身份
    // 2. 验证审核签名
    // 3. 更新报告状态
    // 4. 触发后续流程（更新信誉分、生成交易配额等）
}

// RejectCarbonReport 审核拒绝碳报告
func (c *CarbonContract) RejectCarbonReport(ctx contractapi.TransactionContextInterface,
    reportID string,
    reviewerID string,
    reason string) error {
    // 1. 验证审核员身份
    // 2. 记录拒绝原因
    // 3. 扣除企业信誉分
}

// ==================== 交易相关 ====================

// CreateAuctionOrder 创建拍卖订单
func (c *CarbonContract) CreateAuctionOrder(ctx contractapi.TransactionContextInterface,
    orderID string,
    enterpriseID string,
    orderType string,  // "BUY" or "SELL"
    amount float64,
    price float64) error {
    // 1. 验证企业身份和余额
    // 2. 创建订单
    // 3. 锁定相应资产
}

// ExecuteMatch 执行撮合
func (c *CarbonContract) ExecuteMatch(ctx contractapi.TransactionContextInterface,
    matchID string,
    buyOrderID string,
    sellOrderID string,
    matchedAmount float64,
    matchedPrice float64) error {
    // 1. 验证撮合权限（仅管理员可调用）
    // 2. 验证订单有效性
    // 3. 执行资产转移
    // 4. 更新订单状态
    // 5. 发出交易完成事件
}

// CreateP2PTrade 创建P2P交易
func (c *CarbonContract) CreateP2PTrade(ctx contractapi.TransactionContextInterface,
    tradeID string,
    sellerID string,
    buyerID string,
    amount float64,
    price float64) error {
    // 1. 验证双方身份
    // 2. 验证资产余额
    // 3. 创建待确认交易
}

// ConfirmP2PTrade 确认P2P交易
func (c *CarbonContract) ConfirmP2PTrade(ctx contractapi.TransactionContextInterface,
    tradeID string,
    confirmerID string) error {
    // 1. 验证确认者身份
    // 2. 执行资产转移
    // 3. 更新交易状态
}

// ==================== 查询相关 ====================

// GetCarbonReport 查询碳报告
func (c *CarbonContract) GetCarbonReport(ctx contractapi.TransactionContextInterface,
    reportID string) (*CarbonReport, error) {
    // 返回报告详情
}

// GetEnterpriseHistory 查询企业历史记录
func (c *CarbonContract) GetEnterpriseHistory(ctx contractapi.TransactionContextInterface,
    enterpriseID string) ([]*HistoryRecord, error) {
    // 返回企业所有交易和报告历史
}

// ==================== 信誉分相关 ====================

// UpdateCreditScore 更新信誉分
func (c *CarbonContract) UpdateCreditScore(ctx contractapi.TransactionContextInterface,
    enterpriseID string,
    change int,
    reason string) error {
    // 1. 验证调用权限
    // 2. 更新信誉分
    // 3. 记录变更原因
}

// ==================== 事件定义 ====================

// 事件类型
const (
    EventReportSubmitted    = "REPORT_SUBMITTED"
    EventReportApproved     = "REPORT_APPROVED"
    EventReportRejected     = "REPORT_REJECTED"
    EventOrderCreated       = "ORDER_CREATED"
    EventMatchExecuted      = "MATCH_EXECUTED"
    EventTradeCompleted     = "TRADE_COMPLETED"
    EventCreditScoreChanged = "CREDIT_SCORE_CHANGED"
)

func main() {
    chaincode, err := contractapi.NewChaincode(&CarbonContract{})
    if err != nil {
        panic(err)
    }
    chaincode.Start()
}
```

### 3.2 数据结构定义

```go
// 碳报告结构
type CarbonReport struct {
    ReportID        string    `json:"reportId"`
    EnterpriseID    string    `json:"enterpriseId"`
    ReportPeriod    string    `json:"reportPeriod"`
    TotalEmission   float64   `json:"totalEmission"`
    Status          string    `json:"status"`  // DRAFT, SUBMITTED, APPROVED, REJECTED
    EmissionData    string    `json:"emissionData"`  // JSON格式详细数据
    Signature       string    `json:"signature"`     // 企业数字签名
    ApproverID      string    `json:"approverId"`
    ApprovalSig     string    `json:"approvalSignature"`
    CreatedAt       time.Time `json:"createdAt"`
    ApprovedAt      time.Time `json:"approvedAt"`
}

// 拍卖订单结构
type AuctionOrder struct {
    OrderID       string    `json:"orderId"`
    EnterpriseID  string    `json:"enterpriseId"`
    OrderType     string    `json:"orderType"`  // BUY, SELL
    Amount        float64   `json:"amount"`
    Price         float64   `json:"price"`
    Status        string    `json:"status"`  // PENDING, PARTIAL, FULL, CANCELLED
    MatchedAmount float64   `json:"matchedAmount"`
    CreatedAt     time.Time `json:"createdAt"`
}

// 撮合结果结构
type MatchingResult struct {
    MatchID        string    `json:"matchId"`
    BuyOrderID     string    `json:"buyOrderId"`
    SellOrderID    string    `json:"sellOrderId"`
    MatchedAmount  float64   `json:"matchedAmount"`
    MatchedPrice   float64   `json:"matchedPrice"`
    ExecutedAt     time.Time `json:"executedAt"`
}
```

---

## 4. 后端集成规范

### 4.1 Fabric SDK 集成

```java
// FabricService.java

@Service
public class FabricService {
    
    private final Channel channel;
    private final HFClient client;
    
    /**
     * 提交交易到Fabric网络
     */
    public TransactionResult submitTransaction(String chaincodeName, 
                                                String functionName,
                                                String... args) {
        // 1. 创建交易提案
        // 2. 发送给背书节点
        // 3. 收集背书签名
        // 4. 提交给Orderer
        // 5. 等待交易确认
    }
    
    /**
     * 查询账本数据
     */
    public QueryResult queryLedger(String chaincodeName,
                                    String functionName,
                                    String... args) {
        // 查询Peer节点账本
    }
    
    /**
     * 监听区块事件
     */
    public void registerBlockListener(BlockEventListener listener) {
        // 注册区块事件监听器
    }
}
```

### 4.2 现有服务改造

```java
// BlockchainService 改造

@Service
public class BlockchainServiceImpl implements BlockchainService {
    
    private final FabricService fabricService;
    
    @Override
    public String storeCarbonReport(CarbonReport report) {
        // 改造：调用Fabric Chaincode存储
        TransactionResult result = fabricService.submitTransaction(
            "carbon_cc",
            "SubmitCarbonReport",
            report.getId().toString(),
            report.getEnterpriseId().toString(),
            serializeEmissionData(report),
            report.getDigitalSignature()
        );
        
        return result.getTransactionId();
    }
    
    @Override
    public CarbonReport getCarbonReport(Long reportId) {
        // 改造：从Fabric账本查询
        QueryResult result = fabricService.queryLedger(
            "carbon_cc",
            "GetCarbonReport",
            reportId.toString()
        );
        
        return deserializeReport(result.getPayload());
    }
}
```

---

## 5. 实施路线图

### 5.1 阶段规划

| 阶段 | 内容 | 工时 | 优先级 |
|------|------|------|--------|
| Phase 1 | 本地Fabric网络搭建 | 1周 | P0 |
| Phase 2 | Chaincode开发与测试 | 2周 | P0 |
| Phase 3 | 后端SDK集成 | 1周 | P0 |
| Phase 4 | 现有服务改造 | 2周 | P1 |
| Phase 5 | 测试网部署 | 1周 | P1 |
| Phase 6 | 生产环境部署 | 2周 | P2 |

### 5.2 技术依赖

| 组件 | 版本要求 |
|------|----------|
| Hyperledger Fabric | 2.5.x |
| Fabric CA | 1.5.x |
| Fabric Java SDK | 2.2.x |
| Docker | 20.x+ |
| Kubernetes | 1.25+ (生产环境) |

---

## 6. 验收标准

### 6.1 功能验收

- [ ] 所有碳报告成功上链存储
- [ ] 交易撮合通过Chaincode执行
- [ ] 数字签名验证通过Fabric CA
- [ ] 历史数据可追溯查询

### 6.2 性能验收

| 指标 | 目标值 |
|------|--------|
| 交易提交延迟 | < 3秒 |
| 查询响应时间 | < 1秒 |
| 吞吐量 | > 100 TPS |
| 节点可用性 | 99.9% |

### 6.3 安全验收

- [ ] 所有通信使用TLS加密
- [ ] 节点间身份认证通过MSP
- [ ] 智能合约通过安全审计
- [ ] 私钥安全存储（HSM推荐）

# 项目详细方案 SPEC — 算法模型与系统设计

> 日期: 2026-05-09 | 状态: 已批准 | 来源: docs/raw/03-项目详细方案.md

## 1. 概述

基于原始详细方案文档（123 页），提取算法模型设计、系统架构设计和非功能性需求，与当前代码实现做交叉验证。

---

## 2. 核心算法实现验证

### 2.1 RSA 数字签名算法

| 算法步骤 | 原始设计 | 当前实现 | 状态 |
|----------|---------|---------|------|
| 密钥对生成（p,q → n,e,d） | 基于大素数生成 RSA 密钥对 | `DigitalSignatureService.generateKeyPair()` — Java KeyPairGenerator RSA 2048 | ✅ |
| SHA-256 哈希 | 对文件进行 SHA-256 哈希运算 | `RsaKeyUtil` — SHA256withRSA | ✅ |
| 数字签名（私钥签名） | 企业用私钥对哈希值签名 | `DigitalSignatureService.sign()` | ✅ |
| 签名验证（公钥验签） | 审核员用企业公钥验证签名 | `DigitalSignatureService.verify()` | ✅ |
| RSA 加密（公钥加密） | 用审核员公钥加密敏感数据 | `DigitalSignatureService.encrypt()` | ✅ |
| RSA 解密（私钥解密） | 审核员用自己私钥解密 | `DigitalSignatureService.decrypt()` | ✅ |
| 密钥过期管理 | 密钥版本号、过期时间、状态管理 | `RsaKeyPair` Entity — key_version, expires_at, key_status | ✅ |

### 2.2 双向拍卖撮合算法

| 算法规则 | 原始设计 | 当前实现 | 状态 |
|----------|---------|---------|------|
| 买方按价格降序 | 出价高的优先撮合 | `findByDirectionAndStatusInOrderByPriceDesc` | ✅ |
| 卖方按价格升序 | 出价低的优先撮合 | `findByDirectionAndStatusInOrderByPriceAsc` | ✅ |
| 价格匹配条件 | 买价 ≥ 卖价时撮合 | `buyOrder.getPrice().compareTo(sellOrder.getPrice()) >= 0` | ✅ |
| 成交价计算 | 取买卖双方中间价 | `(buyPrice + sellPrice) / 2` | ✅ |
| 部分匹配 | 支持挂单部分成交 | `matchedQuantity` 跟踪已匹配量 | ✅ |
| 撮合状态更新 | 待匹配 → 部分匹配 → 完全匹配 | `updateOrderStatus()` 自动判断 | ✅ |
| 企业配额更新 | 撮合后更新买卖双方碳配额 | `updateEnterpriseQuota()` | ✅ |
| 交易记录生成 | 撮合成功生成 Transaction 记录 | `transactionRepository.save(trade)` | ✅ |

### 2.3 Fabric 交易模型

| 节点角色 | 原始设计 | 当前实现 | 状态 |
|----------|---------|---------|------|
| 企业用户 → 客户端节点 | 提交交易请求 | 区块链层 Mock | ⚠️ Mock |
| 审核员 → 背书节点 | 验证交易合法性 | 区块链层 Mock | ⚠️ Mock |
| 管理员 → 排序/提交节点 | 网络管理和维护 | 区块链层 Mock | ⚠️ Mock |
| 第三方 → 特殊参与者 | 访问特定交易信息 | 区块链层 Mock | ⚠️ Mock |

### 2.4 信誉分奖惩机制

| 机制 | 原始设计 | 当前实现 | 状态 |
|------|---------|---------|------|
| 初始信誉分 | 企业初始拥有信誉分 | `CreditScore.score = 100` | ✅ |
| 审核扣分 | 数据造假 -20, 迟交 -5, 轻微违规 -10, 严重违规 -30 | `CreditEventTypeEnum` 5 种事件类型 | ✅ |
| 加分奖励 | 良好行为 +5 | `CreditEventTypeEnum.BONUS_GOOD_BEHAVIOR` | ✅ |
| 低分限制 | 分数 < 40 限制交易 | `CreditLevelEnum.WARNING/DANGER` + trade_restricted | ✅ |
| 极低分冻结 | 分数 < 20 冻结账户 | `CreditLevelEnum.FROZEN` + account_frozen | ✅ |

---

## 3. 非功能性需求验证

| 需求 | 原始设计 | 当前实现 | 状态 |
|------|---------|---------|------|
| 可扩展性 | 模块化架构、水平扩展 | Spring Boot 分层架构，16 个独立 Controller | ✅ |
| 可用性 | 容错、自动故障恢复 | 全局异常处理器 `GlobalExceptionHandler` | ⚠️ 部分 |
| 安全性 | 加密传输、身份认证、权限控制 | JWT + BCrypt + RBAC + @PreAuthorize | ✅ |
| 可靠性 | 数据完整性、监控报警 | JPA 事务 + Redis 缓存 + Prometheus/Grafana 监控配置 | ✅ |
| 跨平台性 | 主流浏览器兼容 | Vue 3 + Vite 标准 Web 应用 | ✅ |
| 可重用性 | 可复用组件库 | Element Plus 组件库 + 共享 components | ✅ |
| 可移植性 | 前后端分离 | Vite 代理开发 + Docker Compose 部署 | ✅ |

---

## 4. 系统架构验证

| 层级 | 原始设计 | 当前实现 | 状态 |
|------|---------|---------|------|
| 视图层 | Vue + ECharts | Vue 3.5 + ECharts 6 + Element Plus | ✅ |
| 业务逻辑层 | Spring Boot | Spring Boot 3.2.5 + 19 Service | ✅ |
| 用户验证 | JWT + RBAC | JwtTokenProvider + JwtAuthenticationFilter + 5 角色 | ✅ |
| 区块链底层 | Fabric SDK + 网络层 + 共识层 + 合约层 | Mock BlockchainService | ⚠️ Mock |
| 数据库 | MySQL | MySQL 8 + Flyway 迁移 + 21 张表 | ✅ |
| 数据备份 | 数据备份与还原方案 | Docker volumes 持久化 | ⚠️ 基础 |

---

## 5. Gap 总结

| Gap | 严重度 | 建议 |
|-----|--------|------|
| Fabric 区块链未集成 | 中 | 用户确认为预期未完成 |
| 服务熔断机制未实现 | 低 | 可选，非核心功能 |
| 数据备份仅为 Docker volume | 低 | 生产环境需正式备份方案 |
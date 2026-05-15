# OAISS Chain 双碳链动系统 — 产品说明书

> 版本：1.0.0 | 日期：2026-05-02

---

## 目录

1. [系统概述](#1-系统概述)
2. [技术架构](#2-技术架构)
3. [用户角色与权限](#3-用户角色与权限)
4. [业务模块详细说明](#4-业务模块详细说明)
   - 4.1 用户认证模块
   - 4.2 碳核算模块
   - 4.3 数据审核模块
   - 4.4 碳交易模块
   - 4.5 双向拍卖模块
   - 4.6 碳币交易模块
   - 4.7 信誉评分模块
   - 4.8 数字签名模块
   - 4.9 碳中和项目模块
   - 4.10 碳排放评级模块
   - 4.11 AI智能模块
   - 4.12 区块链模块
   - 4.13 搜索查询模块
   - 4.14 第三方监管模块
   - 4.15 管理后台模块
   - 4.16 用户中心模块
5. [核心业务流程](#5-核心业务流程)
6. [数据库设计](#6-数据库设计)
7. [前端页面说明](#7-前端页面说明)
8. [API接口总览](#8-api接口总览)
9. [需求分级](#9-需求分级)

---

## 1. 系统概述

### 1.1 项目定位

OAISS Chain（双碳链动系统）是一个基于区块链技术的企业碳资产管理平台，面向碳排放核算、碳配额交易、碳中和项目管理等核心业务场景。系统通过 Hyperledger Fabric 区块链确保碳数据的不可篡改性和可追溯性，为碳市场参与者提供可信的数字化基础设施。

### 1.2 核心能力

| 能力领域 | 说明 |
|---------|------|
| 碳核算管理 | 企业碳排放报告的创建、提交、审核全流程管理；基于官方碳核算指南的发电企业和电网企业碳核算模型 |
| 数据审核 | 审核员对核算结果及佐证材料进行人工审核，RSA数字签名验证数据完整性，审核通过后上链存证 |
| 碳配额交易 | 支持双向拍卖撮合和P2P点对点交易双阶段交易模式 |
| 碳币交易市场 | 碳币作为数字货币的发行和流通，碳额度与碳币之间的兑换交易体系，市场价格反映供求关系 |
| 信誉评分体系 | 基于交易行为和合规情况的企业信誉评估，信誉分影响拍卖撮合优先权和交易权限 |
| 数字签名 | RSA密钥对管理，碳报告签名验签，数据加密传输 |
| 碳中和项目 | 碳汇、CCUS、可再生能源等项目全生命周期管理，碳中和激励机制 |
| 碳排放评级 | 基于排放数据的企业碳排放数字评分制度评定与行业排名，评级结果给予奖励或惩罚 |
| 区块链存证 | 关键碳数据上链存证，确保不可篡改，基于Fabric交易模型 |
| AI智能模块 | 市场智能预测（交易情况预测、走势分析）和企业境况智能推断（企业环保性评估、违规超额排放检测） |

### 1.3 业务参与者

系统涉及五类核心参与者：

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   企业用户   │    │   审核员     │    │  认证机构    │
│ (ENTERPRISE) │    │ (REVIEWER)  │    │(AUTHENTICATOR)│
└──────┬──────┘    └──────┬──────┘    └──────┬──────┘
       │                  │                  │
       └──────────┬───────┴──────────────────┘
                  │
          ┌───────┴───────┐
          │  OAISS Chain  │
          │    平台系统    │
          └───────┬───────┘
                  │
       ┌──────────┴──────────┐
       │                     │
┌──────┴──────┐      ┌──────┴──────┐
│   管理员     │      │ 第三方监管   │
│  (ADMIN)    │      │(THIRD_PARTY)│
└─────────────┘      └─────────────┘
```

---

## 2. 技术架构

### 2.1 技术栈

| 层级 | 技术选型 | 版本 |
|------|---------|------|
| **后端框架** | Spring Boot | 3.2.5 |
| **ORM** | Spring Data JPA + Hibernate | 6.x |
| **数据库** | MySQL | 8.x |
| **缓存** | Redis | 7.x |
| **认证** | Spring Security + JWT (jjwt) | 0.12.5 |
| **文件存储** | MinIO | — |
| **数据库迁移** | Flyway | — |
| **区块链** | Hyperledger Fabric | — |
| **前端框架** | Vue 3 + Vite | — |
| **UI组件库** | Element Plus | — |
| **状态管理** | Pinia | — |
| **HTTP客户端** | Axios | — |

### 2.2 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        前端 (Vue 3)                          │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐              │
│  │ 企业 │ │审核员│ │认证  │ │管理  │ │第三方│              │
│  │ 端   │ │ 端   │ │机构端│ │ 端   │ │监管端│              │
│  └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘              │
│     └────────┴────────┴────────┴────────┘                    │
│                    Axios HTTP                                 │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────┴───────────────────────────────────┐
│                   后端 (Spring Boot)                          │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  Spring Security + JWT Filter (认证 & 授权)          │    │
│  └─────────────────────────┬───────────────────────────┘    │
│                            │                                 │
│  ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐        │
│  │16个      │ │ Service  │ │ JPA      │ │ Security │        │
│  │Controller│→│ Layer    │ │ Repository│ │ Module   │        │
│  └─────────┘ └──────────┘ └──────────┘ └──────────┘        │
└─────────┬──────────┬──────────┬──────────────────────────────┘
          │          │          │
    ┌─────┴───┐ ┌────┴────┐ ┌──┴──────────┐
    │  MySQL  │ │  Redis  │ │ Hyperledger │
    │  8.x   │ │  7.x   │ │   Fabric    │
    └─────────┘ └─────────┘ └─────────────┘
```

---

## 3. 用户角色与权限

### 3.1 角色定义

| 角色代码 | userType | 角色名称 | 描述 |
|---------|----------|---------|------|
| ENTERPRISE | 1 | 企业用户 | 碳排放主体，进行碳核算报告提交、碳交易、碳中和项目管理 |
| REVIEWER | 2 | 审核员 | 审核碳排放报告和碳中和项目，管理信誉评分 |
| AUTHENTICATOR | 3 | 认证机构 | 对碳中和项目进行第三方认证 |
| ADMIN | 4 | 管理员 | 系统管理，用户管理，执行撮合，碳币充值，信誉评估，签发准入证书和审核员资格证 |
| THIRD_PARTY | 5 | 第三方监管 | 监管碳报告和交易数据，查看统计信息，定期核查 |

### 3.2 Fabric节点映射

基于Hyperledger Fabric区块链平台，系统将用户角色映射到不同的节点类型：

| 用户类型 | 节点类型 | 描述 |
|---------|---------|------|
| 企业用户 | 客户端 | 与背书节点进行交互，提交交易请求 |
| 数据审核员 | 背书节点 | 验证交易和数据的合法性，确保交易符合业务规则和法律法规 |
| 管理员 | 排序节点/提交对等节点 | 负责网络的管理和维护，包括添加、移除节点、更新智能合约、配置网络参数 |
| 第三方监管机构 | 特殊参与者角色 | 拥有访问特定交易信息的权限，确保交易的合法性和透明性 |

### 3.2 权限矩阵

| 功能模块 | 企业用户 | 审核员 | 认证机构 | 管理员 | 第三方监管 |
|---------|:-------:|:-----:|:-------:|:-----:|:--------:|
| 创建碳报告 | ✅ | ❌ | ❌ | ❌ | ❌ |
| 审核碳报告 | ❌ | ✅ | ❌ | ❌ | ❌ |
| 查看碳报告列表 | ❌ | ✅ | ✅ | ✅ | ✅ |
| 数据审核（RSA验签+上链） | ❌ | ✅ | ❌ | ❌ | ❌ |
| 创建P2P交易 | ✅ | ❌ | ❌ | ❌ | ❌ |
| 双向拍卖挂单 | ✅ | ❌ | ❌ | ❌ | ❌ |
| 执行撮合 | ❌ | ❌ | ❌ | ✅ | ❌ |
| 碳币充值 | ❌ | ❌ | ❌ | ✅ | ❌ |
| 碳币转账 | ✅ | ❌ | ❌ | ✅ | ❌ |
| 扣除信誉分 | ❌ | ✅ | ❌ | ✅ | ❌ |
| 添加奖励分 | ❌ | ❌ | ❌ | ✅ | ❌ |
| 生成密钥对 | ✅ | ✅ | ❌ | ❌ | ✅ |
| 签名碳报告 | ✅ | ❌ | ❌ | ❌ | ❌ |
| 验证签名 | ❌ | ✅ | ❌ | ✅ | ✅ |
| 创建碳中和项目 | ✅ | ❌ | ❌ | ❌ | ❌ |
| 审核碳中和项目 | ❌ | ✅ | ❌ | ✅ | ❌ |
| 核证碳中和项目 | ❌ | ❌ | ✅ | ✅ | ❌ |
| 用户管理 | ❌ | ❌ | ❌ | ✅ | ❌ |
| 区块链状态查询 | ❌ | ❌ | ✅ | ✅ | ✅ |
| 监管数据查询 | ❌ | ❌ | ❌ | ❌ | ✅ |
| 碳排放评级 | ✅ | ✅ | ❌ | ✅ | ❌ |
| AI市场智能预测 | ❌ | ❌ | ❌ | ✅ | ✅ |
| AI企业境况推断 | ❌ | ❌ | ❌ | ✅ | ✅ |
| AI碳排放预测（企业自身） | ✅ | ✅ | ❌ | ✅ | ❌ |
| 签发准入证书 | ❌ | ❌ | ❌ | ✅ | ❌ |
| 签发审核员资格证 | ❌ | ❌ | ❌ | ✅ | ❌ |

---

## 4. 业务模块详细说明

### 4.1 用户认证模块

**API前缀**: `/api/v1/auth`

#### 4.1.1 功能描述

管理用户登录、注册、验证码、Token刷新和登出等认证流程。采用JWT双Token机制（AccessToken + RefreshToken），AccessToken有效期较短用于接口认证，RefreshToken有效期较长用于刷新会话。

#### 4.1.2 接口列表

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/auth/login` | 用户登录（用户名+密码+验证码） | 公开 |
| POST | `/auth/register` | 用户注册 | 公开 |
| POST | `/auth/captcha` | 获取图形验证码（有效期5分钟） | 公开 |
| POST | `/auth/refresh` | 刷新访问令牌（需Refresh-Token头） | 公开 |
| GET | `/auth/check-ip` | 检查当前IP是否在白名单中 | 公开 |
| GET | `/auth/me` | 获取当前用户信息 | 已登录 |
| POST | `/auth/logout` | 用户登出，清除Token缓存 | 已登录 |
| PUT | `/auth/password` | 修改密码（需验证原密码） | 已登录 |

#### 4.1.3 登录流程

```
用户 → 输入账号密码 → 获取图形验证码 → 填写验证码 → 提交登录
  │
  ├→ 验证码校验 → 失败 → 返回错误码 1005
  ├→ IP白名单检查 → 不在白名单 → 返回错误码 1003
  ├→ 用户名密码校验 → 错误 → 返回错误码 401
  └→ 生成JWT → 返回 AccessToken + RefreshToken + 用户信息
```

#### 4.1.4 安全机制

- 密码使用BCrypt加密存储
- 密码字段使用 `@JsonProperty(access = WRITE_ONLY)` 确保不序列化到响应
- 支持IP白名单访问控制
- 图形验证码防暴力破解
- JWT Token支持过期自动失效

---

### 4.2 碳核算模块

**API前缀**: `/api/v1/carbon`

#### 4.2.1 功能描述

企业碳排放报告的全生命周期管理，包括创建、提交、审核、查询和删除。支持季度报告和年度报告两种类型，核算结果包含范围1（直接排放）、范围2（间接排放）、范围3（其他间接排放）三类排放数据。

#### 碳核算模型

系统基于官方碳核算指南构建了两种行业的碳核算模型：

**1. 发电企业碳核算模型**

基于《中国发电企业温室气体排放核算方法与报告指南（试行）》，排放总量计算公式：

```
E = Σ(FCi × NCVi/年 × 10⁻⁶) × (CCi × OFi × 44/12) + Σ(ΣBk,m × 90%) × (EFk,t × 100%) + (AD电 × EF电)
```

原始数据输入项（25项）包括：化石燃料消耗量(FCi)、各类燃料低位热值(NCV)、含碳量(CC)、碳氧化率(OF)、脱硫剂消耗量(Bk,m)、脱硫过程排放因子(EFk,t)、净购入电量(AD电)、区域电网排放因子(EF电)等。

**2. 电网企业碳核算模型**

基于《中国电网企业温室气体排放核算方法与报告指南（试行）》，排放总量计算公式：

```
E = (Σ(REC容量,i - REC回收,i) + Σ(REP容量,j - REP回收,j)) × GWPSF₆ × 10⁻³ + (EL上网 + EL输入 - EL输出 - EL售电) × EF电网
```

原始数据输入项（9项）包括：退役/修理设备SF6容量与回收量、各类电量数据、电网排放因子等。

#### 4.2.2 接口列表

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/carbon/reports` | 创建碳报告（草稿状态） | ENTERPRISE |
| POST | `/carbon/reports/{reportId}/submit` | 提交碳报告至审核 | ENTERPRISE |
| GET | `/carbon/reports/{reportId}` | 获取报告详情 | 已登录 |
| GET | `/carbon/reports` | 查询报告列表（分页） | ADMIN/REVIEWER/AUTHENTICATOR/THIRD_PARTY |
| GET | `/carbon/my-reports` | 查询我的报告 | ENTERPRISE |
| DELETE | `/carbon/reports/{reportId}` | 删除草稿报告 | ENTERPRISE |
| POST | `/carbon/review` | 审核碳报告（通过/驳回） | REVIEWER |

#### 4.2.3 报告状态流转

```
草稿(0) → 已提交(1) → 审核中(2) → 审核通过(3) → 已上链(5)
                  ↓
              审核拒绝(4)
```

- **草稿(0)**: 企业创建报告后的初始状态，可编辑、可删除
- **已提交(1)**: 企业提交后进入此状态
- **审核中(2)**: 审核员开始审核
- **审核通过(3)**: 审核通过，可进行区块链存证
- **审核拒绝(4)**: 审核驳回，企业可修改后重新提交
- **已上链(5)**: 碳数据已写入区块链，不可篡改

#### 4.2.4 碳报告数据结构

| 字段 | 类型 | 说明 |
|------|------|------|
| reportNo | String | 报告编号（唯一） |
| enterpriseId | Long | 提交企业ID |
| accountingPeriod | String | 核算周期（如 2024-Q1） |
| title | String | 报告标题 |
| reportType | Integer | 报告类型（1-季度, 2-年度） |
| emissionData | JSON | 碳排放源数据 |
| totalEmission | BigDecimal | 总排放量（吨CO2当量） |
| scope1Emission | BigDecimal | 直接排放（范围1） |
| scope2Emission | BigDecimal | 间接排放（范围2） |
| scope3Emission | BigDecimal | 其他间接排放（范围3） |
| calculationMethod | String | 核算方法 |
| signatureData | TEXT | RSA数字签名 |
| blockchainTxHash | String | 区块链交易哈希 |
| attachments | JSON | 附件URL列表 |

---

### 4.3 数据审核模块

**API前缀**: `/api/v1/carbon`

#### 4.3.1 功能描述

数据审核员收到经企业用户数字签名后的核算结果，进行RSA解密，验证核算结果来源是否为该企业以及核算结果是否被篡改。验证通过后，数据审核员为该企业提供碳排放报告，并将企业碳报告进行铸造和上链，以确保其可溯源和不可篡改。该模块利用AI与人工审核相结合的模式进行碳核算结果和原始材料的审核。

#### 4.3.2 审核流程

```
企业用户提交碳报告（含RSA数字签名）
  │
  └→ 审核员接收报告
       │
       ├→ RSA解密验证签名 → 确认数据来源真实
       ├→ 验证核算结果是否被篡改
       ├→ 人工审核原始数据和佐证材料
       │
       ├→ 审核通过 → 生成碳排放报告 → 上链存储
       └→ 审核拒绝 → 驳回并说明原因
```

#### 4.3.3 审核相关接口

审核功能已集成在碳核算模块中（详见4.2节接口列表中的审核接口），核心审核操作包括：
- 审核碳报告（通过/驳回）
- 验证数字签名
- 上链存证

---

### 4.4 碳交易模块

**API前缀**: `/api/v1/trade`

#### 4.4.1 功能描述

碳配额交易管理，支持P2P点对点交易和拍卖交易两种模式。交易完成后碳配额在买卖双方之间转移，交易记录可上链存证。

#### 4.4.2 接口列表

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/trade/p2p` | 创建P2P交易 | ENTERPRISE |
| POST | `/trade/auction` | 创建拍卖挂单 | ENTERPRISE |
| POST | `/trade/{tradeId}/confirm` | 确认交易 | ENTERPRISE/ADMIN |
| POST | `/trade/{tradeId}/cancel` | 取消交易 | ENTERPRISE |
| GET | `/trade/{tradeId}` | 获取交易详情 | 已登录 |
| GET | `/trade/list` | 查询交易列表 | ADMIN/REVIEWER/THIRD_PARTY |
| GET | `/trade/my-trades` | 查询我的交易 | ENTERPRISE |

#### 4.4.3 交易状态流转

```
待处理(0) → 处理中(1) → 已完成(2)
     ↓
  已取消(3)
     ↓
  失败(4)
```

#### 4.4.4 P2P交易流程

```
卖方企业 → 创建P2P交易（指定买方、数量、价格）
  │
  ├→ 系统检查卖方可交易碳配额是否充足
  ├→ 冻结卖方对应碳配额
  └→ 生成交易记录（状态：待处理）
       │
       ├→ 买方确认交易 → 碳配额转移 → 交易完成
       └→ 卖方取消交易 → 解冻碳配额 → 交易取消
```

#### 4.4.5 交易数据结构

| 字段 | 类型 | 说明 |
|------|------|------|
| tradeNo | String | 交易编号（唯一） |
| tradeType | Integer | 交易类型（1-拍卖, 2-P2P, 3-配额分配） |
| sellerId | Long | 卖方用户ID |
| buyerId | Long | 买方用户ID |
| quantity | BigDecimal | 交易碳配额数量（吨CO2当量） |
| unitPrice | BigDecimal | 单价（元/吨） |
| totalAmount | BigDecimal | 总金额（元） |
| status | Integer | 交易状态 |
| blockchainTxHash | String | 区块链交易哈希 |

---

### 4.5 双向拍卖模块

**API前缀**: `/api/v1/auction`

#### 4.5.1 功能描述

碳配额双向拍卖市场，采用"价格优先、时间优先"的撮合算法。企业可以提交买入挂单或卖出挂单，系统自动撮合匹配，生成撮合结果。

#### 4.5.2 接口列表

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/auction/buy` | 提交买入挂单 | ENTERPRISE |
| POST | `/auction/sell` | 提交卖出挂单 | ENTERPRISE |
| POST | `/auction/match` | 执行撮合（管理员触发） | ADMIN |
| GET | `/auction/orders` | 查询挂单列表（分页） | 已登录 |
| GET | `/auction/my-orders` | 查询我的挂单 | 已登录 |
| GET | `/auction/results` | 查询撮合结果 | 已登录 |

#### 4.5.3 撮合流程

```
企业A(买入) ──┐                    ┌── 企业C(卖出)
企业B(买入) ──┼→ 挂单池 → 撮合算法 ──┼→ 匹配结果
企业D(卖出) ──┘                    └── 企业E(买入)

撮合规则：
1. 价格优先：买入价 ≥ 卖出价时可撮合
2. 时间优先：同价格挂单按提交时间排序
3. 成交价：取买卖双方挂单价的中间价
```

#### 4.5.4 挂单状态

| 状态码 | 说明 |
|-------|------|
| 0 | 待匹配 |
| 1 | 部分匹配 |
| 2 | 完全匹配 |
| 3 | 已取消 |

#### 4.5.5 撮合结果状态

| 状态码 | 说明 |
|-------|------|
| 0 | 待结算 |
| 1 | 已结算 |
| 2 | 失败 |

---

### 4.6 碳币交易模块

**API前缀**: `/api/v1/carbon-coin`

#### 4.6.1 功能描述

碳币是系统内部的数字货币交易媒介，可作为碳排放权的价值代表和交易媒介。企业可以用人民币充值碳币，用碳币购买碳配额，或出售碳配额获得碳币。碳币支持用户间转账。碳币的市场价格反映碳排放权的供求关系和市场状况，从而引导碳交易的行为和方向。碳币的发行和流通可作为激励减排行为的一种方式，通过设立碳排放目标和配额及发放碳币奖励达成目标，促使更多主体采取减排措施。碳币交易记录通过区块链实现信息的透明和流通监管，确保安全性和可信度，防止双重支付和篡改记录。

#### 4.6.2 接口列表

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/carbon-coin/account` | 获取碳币账户信息 | 已登录 |
| POST | `/carbon-coin/recharge` | 充值碳币 | ADMIN |
| POST | `/carbon-coin/transfer` | 碳币转账 | ENTERPRISE/ADMIN |
| GET | `/carbon-coin/transactions` | 查询交易流水 | 已登录 |

#### 4.6.3 交易类型

| 类型码 | 说明 |
|-------|------|
| 1 | 充值（人民币 → 碳币） |
| 2 | 转账（用户间） |
| 3 | 购买配额（碳币 → 碳配额） |
| 4 | 出售配额（碳配额 → 碳币） |

#### 4.6.4 碳币账户数据结构

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 关联用户ID |
| balance | BigDecimal | 碳币余额 |
| totalRecharged | BigDecimal | 累计充值 |
| totalSpent | BigDecimal | 累计消费 |
| status | Integer | 账户状态（0-禁用, 1-启用） |

---

### 4.7 信誉评分模块

**API前缀**: `/api/v1/credit`

#### 4.7.1 功能描述

基于企业交易行为、合规情况、碳报告质量等维度综合评估企业信誉。信誉分影响企业交易权限：低于阈值被限制交易，严重不足被冻结账户。信誉分排名靠前的企业在双向拍卖撮合阶段享有更大的撮合优先权，政府会对信誉分排名靠前的企业颁发证书及提供奖励。

#### 4.7.2 接口列表

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/credit/my-score` | 获取当前用户信誉分 | ENTERPRISE |
| GET | `/credit/history` | 获取信誉变动历史 | ENTERPRISE |
| GET | `/credit/ranking` | 信誉排名列表 | 已登录 |
| GET | `/credit/{enterpriseId}` | 查询企业信誉分 | 已登录 |
| GET | `/credit/{enterpriseId}/history` | 查询信誉事件历史 | 已登录 |
| POST | `/credit/deduct` | 扣除信誉分 | ADMIN/REVIEWER |
| POST | `/credit/bonus` | 添加奖励分 | ADMIN |
| POST | `/credit/evaluate/{enterpriseId}` | 重新评估信誉等级 | ADMIN |
| GET | `/credit/restricted` | 查询被限制交易的企业 | ADMIN |
| GET | `/credit/frozen` | 查询被冻结的企业 | ADMIN |
| GET | `/credit/check-permission/{enterpriseId}` | 检查交易权限 | 已登录 |

#### 4.7.3 信誉等级体系

| 等级 | 分数范围 | 说明 | 影响 |
|------|---------|------|------|
| EXCELLENT | 80-100 | 优秀 | 正常交易 |
| GOOD | 60-79 | 良好 | 正常交易 |
| WARNING | 40-59 | 预警 | 限制部分交易 |
| DANGER | 20-39 | 危险 | 限制交易 |
| FROZEN | 0-19 | 冻结 | 冻结账户 |

#### 4.7.4 信誉分变动规则

- **扣分场景**: 碳报告审核拒绝、交易违规、数据造假、提供非真实可靠的原始排放数据等
- **加分场景**: 准时提交报告、交易诚信、参与碳中和项目、碳汇行为（植树造林等）等
- **自动评估**: 管理员可手动触发重新评估，系统根据当前分值自动计算等级
- **低于下限处理**: 信誉分低于下限阈值时，管理员将该用户踢出碳交易系统，一定时间后方可重新交易
- **拍卖优先权**: 信誉分排名靠前的企业在双向拍卖撮合阶段享有更大的撮合优先权
- **政府激励**: 政府对信誉分排名靠前的企业颁发证书及提供小奖品奖励

---

### 4.8 数字签名模块

**API前缀**: `/api/v1/signature`

#### 4.8.1 功能描述

基于RSA非对称加密的数字签名系统，用于碳报告数据签名验签和敏感数据加密传输。支持密钥对生成、签名、验签、加密、解密五类操作。

#### 4.8.2 接口列表

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/signature/keypair/generate` | 生成RSA密钥对 | ENTERPRISE/REVIEWER/THIRD_PARTY |
| GET | `/signature/keypair` | 获取当前用户密钥对信息 | ENTERPRISE/REVIEWER/THIRD_PARTY |
| DELETE | `/signature/keypair` | 撤销密钥对 | ENTERPRISE/REVIEWER/THIRD_PARTY |
| POST | `/signature/sign` | 对碳报告数据签名 | ENTERPRISE |
| POST | `/signature/verify` | 验证碳报告签名 | REVIEWER/THIRD_PARTY/ADMIN |
| POST | `/signature/encrypt` | 为审核员加密数据 | ENTERPRISE |
| POST | `/signature/decrypt` | 解密数据 | REVIEWER/ENTERPRISE |

#### 4.8.3 签名流程

```
企业用户 → 生成RSA密钥对 → 用私钥对碳报告数据签名
  │
  └→ 签名数据随碳报告一起存储
       │
       └→ 审核员/第三方 → 用企业公钥验证签名 → 确认数据完整性
```

#### 4.8.4 加密传输流程

```
企业用户 → 用审核员公钥加密敏感数据 → 发送加密数据
  │
  └→ 审核员 → 用自己私钥解密数据 → 查看原始数据
```

#### 4.8.5 密钥对数据结构

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户ID |
| publicKey | TEXT | 公钥（Base64编码） |
| privateKey | TEXT | 私钥（Base64编码，API不返回） |
| keyStatus | Integer | 状态（1-有效, 0-已失效, 2-已过期） |
| expiresAt | DateTime | 过期时间 |
| keyVersion | Integer | 密钥版本号 |
| keyUsage | Integer | 用途（1-签名验签, 2-加密解密, 3-通用） |

---

### 4.9 碳中和项目模块

**API前缀**: `/api/v1/carbon-neutral`

#### 4.9.1 功能描述

碳中和项目全生命周期管理，支持碳汇、CCUS（碳捕集利用与封存）、可再生能源、节能改造等类型项目。项目经历创建、审核、实施、核证、认证、监测六个阶段。系统设有碳中和激励机制：个人或企业可进行碳中和行为（如植树造林等），提供相应佐证材料，管理员根据证明发放信誉分或小奖品作为奖励。同时在碳交易市场上推出优惠政策，对持有碳中和证书的企业提供折扣或补贴，降低参与门槛，促进市场活跃度。

#### 4.9.2 接口列表

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/carbon-neutral` | 创建项目 | 已登录 |
| PUT | `/carbon-neutral/{id}` | 更新项目 | 已登录 |
| GET | `/carbon-neutral/{id}` | 获取项目详情 | 已登录 |
| GET | `/carbon-neutral/search` | 搜索项目 | 已登录 |
| GET | `/carbon-neutral/my` | 我的项目 | 已登录 |
| POST | `/carbon-neutral/{id}/submit` | 提交审核 | 已登录 |
| POST | `/carbon-neutral/{id}/review` | 审核项目 | ADMIN/REVIEWER |
| POST | `/carbon-neutral/{id}/start` | 启动实施 | 已登录 |
| POST | `/carbon-neutral/{id}/submit-verification` | 申请核证 | 已登录 |
| POST | `/carbon-neutral/verify` | 核证项目 | VERIFIER/ADMIN |
| POST | `/carbon-neutral/{id}/use-credits` | 使用碳信用 | 已登录 |
| PUT | `/carbon-neutral/{id}/monitoring` | 更新监测数据 | 已登录 |
| POST | `/carbon-neutral/{id}/apply-certification` | 申请认证 | 已登录 |
| POST | `/carbon-neutral/{id}/certify` | 完成认证 | CERTIFIER/ADMIN |
| POST | `/carbon-neutral/{id}/terminate` | 终止项目 | 已登录 |
| GET | `/carbon-neutral/pending-verification` | 待核证项目 | VERIFIER/ADMIN |

#### 4.9.3 项目生命周期

```
创建申请(筹备) → 提交审核 → 审核通过 → 启动实施 → 申请核证 → 核证完成 → 申请认证 → 认证完成 → 持续监测
     ↓              ↓                                                      ↓
  可编辑/删除     审核拒绝                                               认证拒绝
                    ↓                                                      ↓
                 修改后重提                                             修改后重申请
```

#### 4.9.4 项目类型

| 类型码 | 说明 |
|-------|------|
| 1 | 碳汇（林业碳汇、海洋碳汇等） |
| 2 | CCUS（碳捕集利用与封存） |
| 3 | 可再生能源（风电、光伏等） |
| 4 | 节能改造 |
| 5 | 其他 |

#### 4.9.5 项目状态

| 状态码 | 说明 |
|-------|------|
| 0 | 筹备 |
| 1 | 待审核 |
| 2 | 审核通过 |
| 3 | 实施中 |
| 4 | 已完成 |
| 5 | 已终止 |
| 6 | 审核拒绝 |

---

### 4.10 碳排放评级模块

**API前缀**: `/api/v1/emission`

#### 4.10.1 功能描述

基于企业年度碳排放数据进行数字评分制度评定，提供行业排名和AI碳排放趋势预测功能。评级结果影响企业的碳市场准入和交易限额，根据评级结果给予奖励或惩罚，从而激励企业采取减排措施。碳排放评级可提高碳交易市场的透明度和公平性，增强市场参与者对企业的信任和可信度，为政府和企业提供科学依据。

#### 4.10.2 接口列表

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/emission/ratings/{enterpriseId}` | 企业评级历史 | 已登录 |
| POST | `/emission/ratings` | 生成评级 | 已登录 |
| GET | `/emission/rankings/{year}` | 行业排名 | 已登录 |
| POST | `/emission/predict` | AI碳排放预测 | 已登录 |

#### 4.10.3 评级等级

| 等级 | 说明 | 标准 |
|------|------|------|
| A | 优秀 | 碳排放强度低，减排效果显著 |
| B | 良好 | 碳排放强度中等，有减排措施 |
| C | 一般 | 碳排放强度较高，需改进 |
| D | 较差 | 碳排放强度高，需重点整改 |
| E | 极差 | 碳排放严重超标 |

#### 4.10.4 评级数据结构

| 字段 | 类型 | 说明 |
|------|------|------|
| enterpriseId | Long | 企业ID |
| ratingYear | String | 评级年度 |
| totalEmission | BigDecimal | 碳排放总量（吨CO2当量） |
| emissionIntensity | BigDecimal | 碳排放强度（吨CO2/万元产值） |
| ratingLevel | String | 评级等级（A/B/C/D/E） |
| ratingScore | Integer | 评级分数（0-100） |
| percentileRank | Integer | 同行业排名百分位 |
| reductionRatio | BigDecimal | 减排比例（较上一年） |

---

### 4.11 AI智能模块

**API前缀**: `/api/v1/emission`（预测接口）及 `/api/v1/third-party`（监管预测）

#### 4.11.1 功能描述

AI智能模块包含两大核心功能：市场智能预测和企业境况智能推断。该模块基于近期数据进行预测分析，辅助管理员及第三方审查机构进行市场监控和宏观调控，保证碳交易市场有序开展。对企业的AI碳排放预测和建议均以可视化图表呈现，增强用户体验感。

#### 4.11.2 市场智能预测

该功能基于近期数据进行未来一定时间内的市场交易情况预测，包括挂牌量、挂牌价、成交量、成交价数据及走势、新注册用户数量等信息，辅助管理员及第三方审查机构进行市场监控和宏观调控，保证碳交易市场有序开展。

- 输入：过去一周市场信息（交易量、交易价、挂牌量、挂牌价等）
- 输出：预测结果，以图表与表格相结合的形式进行展示
- 权限：管理员、第三方监管

#### 4.11.3 企业境况智能推断

该功能基于某企业账户历史活动情况和所在省份、行业等客观信息，智能推断该企业发展状况及生产的环保性，评估是否违规、超额排放。

- 输入：企业账户历史信息（交易量、交易价、挂牌量、挂牌价等）
- 输出：预测结果，以图表与表格相结合的形式进行展示
- 权限：管理员、第三方监管

#### 4.11.4 企业自身碳排放预测

基于企业历史碳排放信息，AI分析并预测企业未来几月内的碳排放量，同时给出原始数据排放建议。

- 输入：企业历史碳排放数据
- 输出：预测排放趋势图和建议
- 权限：企业用户、审核员、管理员

---

### 4.12 区块链模块

**API前缀**: `/api/v1/blockchain`

#### 4.12.1 功能描述

基于Hyperledger Fabric的区块链存证模块，提供区块链网络状态查询、区块信息查询、链上交易记录查询等功能。关键碳数据（碳报告、交易记录）上链后不可篡改。

#### 4.12.2 接口列表

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/blockchain/status` | 检查区块链连接状态 | ADMIN/AUTHENTICATOR |
| GET | `/blockchain/block/{blockNumber}` | 查询区块信息 | ADMIN/AUTHENTICATOR/THIRD_PARTY |
| GET | `/blockchain/transaction/{txHash}` | 查询链上交易 | 已登录 |
| GET | `/blockchain/transactions` | 链上交易列表 | 已登录 |
| GET | `/blockchain/blocks/latest` | 最新区块列表 | 已登录 |

#### 4.12.3 上链数据类型

- 碳排放报告的核心数据摘要
- 碳交易记录的关键字段
- 碳中和项目的认证信息
- 信誉评分变动记录

---

### 4.13 搜索查询模块

**API前缀**: `/api/v1/search`

#### 4.13.1 功能描述

提供碳报告搜索、交易记录搜索和市场宏观数据统计功能。支持关键字模糊匹配和多条件筛选。

#### 4.13.2 接口列表

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/search/reports` | 搜索碳报告 | 已登录 |
| GET | `/search/trades` | 搜索交易记录 | 已登录 |
| GET | `/search/market-overview` | 市场概览 | 已登录 |

#### 4.13.3 市场概览数据

- 碳交易总量
- 平均碳价
- 活跃企业数
- 交易笔数
- 碳排放趋势

---

### 4.14 第三方监管模块

**API前缀**: `/api/v1/third-party`

#### 4.14.1 功能描述

第三方监管机构专用模块，提供监管视角的碳报告查询、监管统计和机构信息管理功能。

#### 4.14.2 接口列表

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/third-party/org-info` | 获取当前机构信息 | THIRD_PARTY |
| GET | `/third-party/carbon-reports` | 查询碳报告（监管视角） | THIRD_PARTY |
| GET | `/third-party/statistics` | 获取监管统计数据 | THIRD_PARTY |
| PUT | `/third-party/contact` | 更新机构联系方式 | THIRD_PARTY |

#### 4.14.3 监管统计数据

- 审核报告总数
- 通过率
- 活跃企业数
- 交易笔数

---

### 4.15 管理后台模块

**API前缀**: `/api/v1/admin`

#### 4.15.1 功能描述

管理员后台管理模块，包括用户管理、仪表板数据、系统统计、签发准入证书、签发审核员资格证、信誉分管理等功能。管理员同时担任Fabric区块链的排序节点和提交对等节点，负责网络的管理和维护。

#### 4.15.2 接口列表

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/admin/users` | 查询用户列表（分页） | ADMIN |
| PUT | `/admin/users/{userId}/status` | 更新用户状态（启用/禁用） | ADMIN |
| GET | `/admin/dashboard` | 获取仪表板数据 | ADMIN |
| GET | `/admin/statistics` | 获取系统统计数据 | ADMIN |

#### 4.15.3 仪表板数据

- 总用户数
- 活跃用户数
- 各类型用户数量
- 注册趋势

---

### 4.16 用户中心模块

**API前缀**: `/api/v1/user`

#### 4.16.1 功能描述

用户个人资料管理，包括查看和更新个人信息、修改密码、检查用户名和邮箱可用性。

#### 4.16.2 接口列表

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/user/profile` | 获取当前用户信息 | 已登录 |
| GET | `/user/{userId}` | 根据ID获取用户信息 | 已登录 |
| PUT | `/user/profile` | 更新用户资料 | 已登录 |
| PUT | `/user/password` | 修改密码 | 已登录 |
| GET | `/user/check-username` | 检查用户名是否可用 | 公开 |
| GET | `/user/check-email` | 检查邮箱是否可用 | 公开 |

---

## 5. 核心业务流程

### 5.1 碳核算完整流程

```
┌─────────────────────────────────────────────────────────────────┐
│                        碳核算完整流程                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  企业用户                    审核员                 区块链        │
│    │                          │                     │           │
│    ├─ 创建碳报告(草稿) ──→    │                     │           │
│    ├─ 填写排放数据      ──→   │                     │           │
│    ├─ 数字签名          ──→   │                     │           │
│    ├─ 提交审核          ──→   │                     │           │
│    │                          ├─ 接收报告           │           │
│    │                          ├─ 审核排放数据       │           │
│    │                          ├─ 验证数字签名       │           │
│    │                          │                     │           │
│    │                          ├─ 审核通过 ────────→ │           │
│    │                          │                     ├─ 数据上链  │
│    │                          │                     │           │
│    │←─────────────────────────┤                     │           │
│    │  通知审核结果             │                     │           │
│    │                          │                     │           │
└────┴──────────────────────────┴─────────────────────┴───────────┘
```

### 5.2 碳交易完整流程

```
┌─────────────────────────────────────────────────────────────────┐
│                        碳交易完整流程                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  卖方企业                    系统                  买方企业      │
│    │                          │                     │           │
│    ├─ 创建P2P交易/挂单  ──→  │                     │           │
│    │                          ├─ 检查配额充足       │           │
│    │                          ├─ 冻结卖方配额       │           │
│    │                          │                     │           │
│    │                          ├─ 撮合匹配(拍卖) ──→ │           │
│    │                          │  或 通知买方(P2P)   │           │
│    │                          │                     │           │
│    │                          │←── 买方确认 ────────┤           │
│    │                          │                     │           │
│    │                          ├─ 碳配额转移         │           │
│    │                          ├─ 碳币结算           │           │
│    │                          ├─ 更新信誉分         │           │
│    │                          ├─ 交易记录上链       │           │
│    │                          │                     │           │
│    │←─────────────────────────┤←────────────────────┤           │
│    │  通知交易完成             │  通知交易完成       │           │
│    │                          │                     │           │
└────┴──────────────────────────┴─────────────────────┴───────────┘
```

### 5.3 碳中和项目完整流程

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       碳中和项目完整流程                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  企业用户        管理员/审核员      第三方核证      认证机构              │
│    │                │                │               │                   │
│    ├─ 创建项目 ──→ │                │               │                   │
│    ├─ 提交审核 ──→ │                │               │                   │
│    │                ├─ 审核项目      │               │                   │
│    │←── 审核通过 ──┤                │               │                   │
│    ├─ 启动实施      │                │               │                   │
│    │   ...实施中... │                │               │                   │
│    ├─ 申请核证 ──────────────────→ │               │                   │
│    │                │                ├─ 核证减排量   │                   │
│    │                │                ├─ 签发碳信用   │                   │
│    │←───────────────────────────────┤               │                   │
│    ├─ 申请认证 ────────────────────────────────────→│                   │
│    │                │                │               ├─ 认证审核         │
│    │←───────────────────────────────────────────────┤                   │
│    ├─ 更新监测数据  │                │               │                   │
│    ├─ 使用碳信用    │                │               │                   │
│    │                │                │               │                   │
└────┴────────────────┴────────────────┴───────────────┴───────────────────┘
```

### 5.4 信誉评分联动流程

```
┌─────────────────────────────────────────────────────────────────┐
│                       信誉评分联动流程                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  触发事件                          系统响应                       │
│    │                                │                            │
│    ├─ 碳报告审核拒绝 ──→           ├─ 扣除信誉分                 │
│    ├─ 交易违规       ──→           ├─ 扣除信誉分                 │
│    ├─ 数据造假       ──→           ├─ 扣除信誉分                 │
│    │                                │                            │
│    │                                ├─ 重新评估信誉等级           │
│    │                                │                            │
│    │                                ├─ 分数 < 40 → 限制交易      │
│    │                                ├─ 分数 < 20 → 冻结账户      │
│    │                                │                            │
│    ├─ 准时提交报告   ──→           ├─ 增加信誉分                 │
│    ├─ 交易诚信       ──→           ├─ 增加信誉分                 │
│    │                                │                            │
└────┴────────────────────────────────┴────────────────────────────┘
```

---

## 6. 数据库设计

### 6.1 数据库概览

系统共包含 **21张业务表**，按模块分布如下：

| 模块 | 表名 | 说明 |
|------|------|------|
| **用户认证** | user | 用户表 |
| | enterprise | 企业信息表 |
| | reviewer | 审核员信息表 |
| | reviewer_qualification | 审核员资质表 |
| | authenticator | 认证机构表 |
| | third_party_org | 第三方监管机构表 |
| | account_permission_list | 账户权限表 |
| | entry_permission | 权限条目表 |
| | user_type_list | 用户类型表 |
| **碳核算** | carbon_report | 碳报告表 |
| **碳交易** | transaction | 交易记录表 |
| | auction_order | 拍卖挂单表 |
| | matching_result | 撮合结果表 |
| **碳币** | carbon_coin_account | 碳币账户表 |
| | carbon_coin_transaction | 碳币交易流水表 |
| **信誉评分** | credit_score | 信誉评分表 |
| | credit_event | 信誉事件表 |
| **数字签名** | rsa_key_pair | RSA密钥对表 |
| **碳排放评级** | emission_rating | 碳排放评级表 |
| **碳中和** | carbon_neutral_project | 碳中和项目表 |
| **操作日志** | operation_log | 操作日志表 |

### 6.2 核心表结构

#### user 表（用户表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 用户名 |
| password | VARCHAR(255) | NOT NULL | 密码（BCrypt） |
| phone | VARCHAR(20) | UNIQUE | 手机号 |
| email | VARCHAR(100) | — | 邮箱 |
| real_name | VARCHAR(50) | — | 真实姓名 |
| user_type | INT | NOT NULL | 用户类型（1-5） |
| status | INT | NOT NULL, DEFAULT 1 | 状态（0-禁用, 1-启用） |
| allowed_ips | TEXT | — | IP白名单（JSON） |
| last_login_at | DATETIME | — | 最后登录时间 |
| last_login_ip | VARCHAR(50) | — | 最后登录IP |
| avatar | VARCHAR(500) | — | 头像URL |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| is_deleted | BOOLEAN | NOT NULL | 逻辑删除标记 |

#### enterprise 表（企业信息表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 主键 |
| user_id | BIGINT | UNIQUE, NOT NULL | 关联用户ID |
| enterprise_name | VARCHAR(200) | NOT NULL | 企业名称 |
| credit_code | VARCHAR(18) | UNIQUE, NOT NULL | 统一社会信用代码 |
| address | VARCHAR(500) | — | 企业地址 |
| contact_person | VARCHAR(50) | — | 联系人 |
| contact_phone | VARCHAR(20) | — | 联系电话 |
| industry | VARCHAR(100) | — | 所属行业 |
| scale | VARCHAR(50) | — | 企业规模 |
| carbon_quota | DECIMAL(15,4) | NOT NULL, DEFAULT 0 | 碳配额余额 |
| carbon_used | DECIMAL(15,4) | NOT NULL, DEFAULT 0 | 已使用碳配额 |
| carbon_tradable | DECIMAL(15,4) | NOT NULL, DEFAULT 0 | 可交易碳配额 |
| license_url | VARCHAR(500) | — | 营业执照URL |
| cert_status | INT | NOT NULL, DEFAULT 0 | 认证状态（0-3） |

#### carbon_report 表（碳报告表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 主键 |
| report_no | VARCHAR(50) | UNIQUE, NOT NULL | 报告编号 |
| enterprise_id | BIGINT | NOT NULL | 企业ID |
| submitter_id | BIGINT | NOT NULL | 提交用户ID |
| accounting_period | VARCHAR(20) | NOT NULL | 核算周期 |
| title | VARCHAR(200) | NOT NULL | 报告标题 |
| report_type | INT | NOT NULL | 类型（1-季度, 2-年度） |
| emission_data | TEXT | NOT NULL | 排放数据（JSON） |
| total_emission | DECIMAL(15,4) | — | 总排放量 |
| scope1_emission | DECIMAL(15,4) | — | 范围1排放 |
| scope2_emission | DECIMAL(15,4) | — | 范围2排放 |
| scope3_emission | DECIMAL(15,4) | — | 范围3排放 |
| calculation_method | VARCHAR(100) | — | 核算方法 |
| status | INT | NOT NULL, DEFAULT 0 | 状态（0-5） |
| reviewer_id | BIGINT | — | 审核员ID |
| review_comment | TEXT | — | 审核意见 |
| reviewed_at | DATETIME | — | 审核时间 |
| signature_data | TEXT | — | RSA签名 |
| blockchain_tx_hash | VARCHAR(255) | — | 区块链交易哈希 |
| on_chain_at | DATETIME | — | 上链时间 |
| attachments | TEXT | — | 附件URL（JSON） |

#### transaction 表（交易记录表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 主键 |
| trade_no | VARCHAR(50) | UNIQUE, NOT NULL | 交易编号 |
| trade_type | INT | NOT NULL | 类型（1-拍卖, 2-P2P, 3-配额分配） |
| seller_id | BIGINT | NOT NULL | 卖方ID |
| buyer_id | BIGINT | NOT NULL | 买方ID |
| quantity | DECIMAL(15,4) | NOT NULL | 交易数量（吨） |
| unit_price | DECIMAL(15,2) | NOT NULL | 单价（元/吨） |
| total_amount | DECIMAL(15,2) | NOT NULL | 总金额（元） |
| report_id | BIGINT | — | 关联碳报告ID |
| status | INT | NOT NULL, DEFAULT 0 | 状态（0-4） |
| remark | TEXT | — | 备注 |
| blockchain_tx_hash | VARCHAR(255) | — | 区块链交易哈希 |
| completed_at | DATETIME | — | 完成时间 |

### 6.3 实体关系图

```
user ──1:1──→ enterprise
user ──1:1──→ reviewer
user ──1:1──→ authenticator
user ──1:1──→ third_party_org
user ──1:1──→ carbon_coin_account
user ──1:1──→ rsa_key_pair

enterprise ──1:N──→ carbon_report
enterprise ──1:1──→ credit_score
enterprise ──1:N──→ credit_event
enterprise ──1:N──→ emission_rating
enterprise ──1:N──→ carbon_neutral_project

carbon_report ──1:N──→ transaction
transaction ──1:1──→ matching_result

auction_order ──N:1──→ user
matching_result ──N:1──→ auction_order (buy_order)
matching_result ──N:1──→ auction_order (sell_order)

carbon_coin_account ──1:N──→ carbon_coin_transaction
```

---

## 7. 前端页面说明

### 7.1 页面路由结构

| 路由 | 页面文件 | 角色 | 说明 |
|------|---------|------|------|
| `/official-home` | OfficialHome.vue | 公开 | 官网首页 |
| `/login` | Login.vue | 公开 | 登录页 |
| `/enterprise/carbon/upload` | CarbonUpload.vue | ENTERPRISE | 碳核算-上传审核 |
| `/enterprise/orders/manage` | OrdersManage.vue | ENTERPRISE | P2P订单管理 |
| `/enterprise/trading/market` | TradingMarket.vue | ENTERPRISE | 双向拍卖市场 |
| `/enterprise/trading/p2p` | TradingP2P.vue | ENTERPRISE | P2P交易 |
| `/enterprise/company/dashboard` | CompanyDashboard.vue | ENTERPRISE | 数据可视化 |
| `/enterprise/credit/score` | CreditScore.vue | ENTERPRISE | 信誉评分 |
| `/enterprise/carbon-coin/account` | CarbonCoin.vue | ENTERPRISE | 碳币账户 |
| `/enterprise/blockchain/browser` | Blockchain.vue | ENTERPRISE | 区块链浏览器 |
| `/enterprise/carbon-neutral/projects` | CarbonNeutral.vue | ENTERPRISE | 碳中和项目列表 |
| `/enterprise/carbon-neutral/projects/:id` | CarbonNeutralDetail.vue | ENTERPRISE | 项目详情 |
| `/enterprise/emission/data` | EmissionData.vue | ENTERPRISE | 排放数据 |
| `/enterprise/user/profile` | UserProfile.vue | ENTERPRISE | 个人中心 |
| `/auditor/audit/list` | AuditList.vue | REVIEWER | 碳排放数据审核 |
| `/authenticator/verify/list` | VerifyList.vue | AUTHENTICATOR | 认证列表 |
| `/third-party/monitor` | Monitor.vue | THIRD_PARTY | 监管面板 |
| `/admin/system/users` | SystemUsers.vue | ADMIN | 用户管理 |
| `/admin/system/carbon` | SystemCarbon.vue | ADMIN | 碳核算管理 |
| `/admin/system/config` | SystemConfig.vue | ADMIN | 系统配置 |
| `/admin/data/statistics` | DataStatistics.vue | ADMIN | 统计数据 |

### 7.2 侧边栏菜单结构

#### 企业用户菜单

```
碳核算
  └─ 上传审核
P2P订单管理
碳交易
  ├─ 双向拍卖
  └─ P2P交易
本公司信息
信誉评分
碳币账户
区块链
  └─ 区块链浏览器
碳中和
  └─ 碳中和项目
个人中心
```

#### 管理员菜单

```
系统管理
  ├─ 用户管理
  ├─ 碳核算管理
  └─ 系统配置
碳交易管理
  └─ 统计数据
```

### 7.3 前端API模块映射

| API模块文件 | 对应后端Controller | 说明 |
|------------|-------------------|------|
| api/auth.js | AuthController | 认证相关 |
| api/user.js | UserController | 用户中心 |
| api/carbon.js | CarbonController | 碳核算 |
| api/trade.js | TradeController | 碳交易 |
| api/auction.js | DoubleAuctionController | 双向拍卖 |
| api/carbonCoin.js | CarbonCoinController | 碳币交易 |
| api/credit.js | CreditScoreController | 信誉评分 |
| api/signature.js | DigitalSignatureController | 数字签名 |
| api/carbonNeutral.js | CarbonNeutralProjectController | 碳中和项目 |
| api/blockchain.js | BlockchainController | 区块链 |
| api/emission.js | EmissionController | 碳排放评级 |
| api/search.js | SearchController | 搜索查询 |
| api/thirdParty.js | ThirdPartyController | 第三方监管 |
| api/admin.js | AdminController | 管理后台 |
| api/file.js | FileController | 文件管理 |
| api/captcha.js | CaptchaController | 验证码 |
| api/request.js | — | Axios实例配置 |

---

## 8. API接口总览

### 8.1 接口统计

| 模块 | Controller | 接口数 |
|------|-----------|--------|
| 认证管理 | AuthController | 8 |
| 用户中心 | UserController | 6 |
| 碳核算管理 | CarbonController | 7 |
| 碳交易管理 | TradeController | 7 |
| 信誉评分管理 | CreditScoreController | 11 |
| 数字签名 | DigitalSignatureController | 7 |
| 管理后台 | AdminController | 4 |
| 区块链管理 | BlockchainController | 5 |
| 碳币交易管理 | CarbonCoinController | 4 |
| 双向拍卖管理 | DoubleAuctionController | 6 |
| 碳中和项目管理 | CarbonNeutralProjectController | 16 |
| 碳排放评级管理 | EmissionController | 4 |
| 搜索查询管理 | SearchController | 3 |
| 第三方监管管理 | ThirdPartyController | 4 |
| 文件管理 | FileController | 11 |
| 验证码 | CaptchaController | 4 |
| **合计** | **16个Controller** | **79个接口** |

### 8.2 统一响应格式

所有API接口统一返回以下格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

错误码定义：

| 错误码 | 说明 |
|-------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或Token过期 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 1001 | 用户名已存在 |
| 1002 | 邮箱已被注册 |
| 1003 | IP不在白名单中 |
| 1005 | 验证码错误或已过期 |
| 5001 | 密钥生成失败 |
| 5002 | 密钥对不存在 |
| 5003 | 密钥已失效或过期 |
| 5004 | 签名失败 |
| 5006 | 验签过程出错 |
| 5007 | 加密失败 |
| 5008 | 解密失败 |

### 8.3 认证方式

所有需要认证的接口使用 Bearer Token 方式：

```
Authorization: Bearer <JWT AccessToken>
```

Token刷新使用自定义Header：

```
Refresh-Token: <JWT RefreshToken>
```

---

## 9. 需求分级

基于项目需求分析文档，系统各功能模块按优先级分为A、B、C三级：

| 需求ID | 需求名称 | 分级 | 说明 |
|-------|---------|------|------|
| 3.1.1 | 智能核算 | A | 核心功能，基于碳核算模型进行企业碳排放量核算 |
| 3.1.2 | 数据审核 | B | 审核员对核算结果进行人工审核和RSA验签 |
| 3.2.1 | 双向拍卖阶段 | A | 核心功能，碳交易第一阶段智能撮合 |
| 3.2.2 | P2P交易阶段 | C | 辅助功能，双向拍卖未出清后的补充交易方式 |
| 3.3.1 | 用户账户历史 | A | 核心功能，交易记录和碳排放历史查询 |
| 3.3.2 | 市场信息查询 | B | 市场宏观数据和统计信息查询 |
| 3.4.1 | 用户管理 | A | 核心功能，管理员对系统用户的管理 |
| 3.5.1 | 用户登录 | A | 核心功能，用户身份验证 |
| 3.6.1 | 市场智能预测 | A | 核心功能，AI预测市场交易走势 |
| 3.6.2 | 企业境况智能推断 | A | 核心功能，AI推断企业环保性和违规检测 |

---

> 本文档基于代码库和原始需求文档（01-项目需求分析、02-项目概要介绍、03-项目详细方案、04-碳核算模型介绍文档、05-项目测试文档）综合生成，涵盖 OAISS Chain 系统全部 16 个后端控制器、79 个API接口、21 张数据库表、20 个前端页面的完整业务说明。

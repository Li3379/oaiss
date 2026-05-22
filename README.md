# OAISS CHAIN — 双碳链动系统

基于区块链的可信碳核算与交易平台，实现碳排放报告、碳交易、信用评分、AI 智能预测等全链路数字化管理。

## 项目概览

OAISS CHAIN 是一个面向双碳（碳达峰、碳中和）领域的全栈 Web 平台，覆盖企业碳核算、审核员审核、碳交易（P2P + 双向拍卖）、碳币经济、信用评分、数字签名、碳中和项目管理、AI 智能预测、区块链存证等核心业务。系统采用四角色模型，支持企业、审核员、第三方监管和管理员协同工作。

### 核心特性

- **碳核算管理** — 企业上报碳排放数据，审核员审核，状态流转（草稿→待审→通过/驳回）
- **碳交易引擎** — P2P 点对点交易 + 双向拍卖撮合两种模式
- **碳币经济** — 平台代币体系，支持充值、转账、交易结算
- **信用评分** — 基于排放数据的 A-E 五级信用评级，影响交易权限
- **数字签名** — RSA 密钥对生成、报告签名/验签、数据加解密
- **碳中和项目** — 减排项目全生命周期管理（注册→认证→监测→核证）
- **AI 智能预测** — 碳排放预测（Prophet）、企业合规推断（IsolationForest + XGBoost）、市场趋势预测
- **区块链存证** — Hyperledger Fabric 链上存储碳报告与交易记录
- **排放评级** — 年度企业碳排放评级与行业排名
- **GB/T 32150 碳核算公式** — 电网排放因子与发电企业专用计算器

## 技术栈

| 层级 | 技术 |
|------|------|
| **后端** | Java 17, Spring Boot 3.2.5, Spring Data JPA, Spring Security, Spring Cache |
| **数据层** | MySQL 8, Redis 7 (Lettuce), MinIO, Flyway |
| **认证** | JWT (jjwt 0.12.5), BCrypt, CSRF |
| **区块链** | Hyperledger Fabric Gateway SDK 1.7.1, Go Chaincode |
| **AI/ML** | FastAPI, Prophet, scikit-learn, XGBoost |
| **前端** | Vue 3.5, TypeScript, Vite, Element Plus 2.13, Pinia 3, Vue Router 5 |
| **可视化** | ECharts 6 (按需引入) |
| **国际化** | vue-i18n 11 (中/英) |
| **API 文档** | SpringDoc OpenAPI 2.5 (Swagger UI) |
| **测试** | JUnit 5 + Testcontainers + JaCoCo (90% 行覆盖), Vitest + Playwright |
| **质量** | PMD, SpotBugs, Resilience4j 熔断 |
| **监控** | Spring Actuator, Micrometer + Prometheus, Logstash JSON 日志 |
| **部署** | Docker Compose (MySQL, Redis, MinIO, Backend, Frontend, ML Service) |

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (Vue 3 + Vite)                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │
│  │ Enterprise│ │ Reviewer │ │  Admin   │ │ Third-Party  │   │
│  │  16 pages │ │  3 pages │ │  6 pages │ │   1 page     │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘   │
│         │              Axios + JWT Auto-Refresh              │
└────────────────────────────┬────────────────────────────────┘
                             │ /api/v1
┌────────────────────────────▼────────────────────────────────┐
│                  Backend (Spring Boot 3.2.5)                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Security Layer: JWT Filter → Role-based Access      │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │  AOP: @AuditLog @RateLimit @DataIsolation            │   │
│  │       @DistributedLock @RequirePermission            │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │  21 Controllers → 31 Services → 22 Repositories      │   │
│  └──────────────────────────────────────────────────────┘   │
│         │              │              │              │        │
│    ┌────▼────┐   ┌────▼────┐   ┌────▼────┐   ┌────▼────┐  │
│    │  MySQL  │   │  Redis  │   │  MinIO  │   │ML Service│  │
│    │   8.x   │   │   7.x   │   │         │   │ (FastAPI)│  │
│    └─────────┘   └─────────┘   └─────────┘   └─────────┘  │
│                                                    │         │
│  ┌─────────────────────────────────────────────────▼──────┐ │
│  │           Hyperledger Fabric Network (Optional)         │ │
│  │           Go Chaincode: carbon-chaincode                │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## 项目结构

```
OAISS CHAIN/
├── oaiss-chain-backend/          # Spring Boot 后端
│   └── src/main/java/com/oaiss/chain/
│       ├── controller/           # 21 REST 控制器
│       ├── service/              # 28 业务服务 + 3 ML 客户端
│       ├── repository/           # 22 JPA 仓库
│       ├── entity/               # 24 JPA 实体
│       ├── dto/                  # 请求/响应 DTO
│       ├── config/               # 17 配置类 (Security, Redis, MinIO, Fabric, ML...)
│       ├── annotation/           # 5 自定义注解 (@AuditLog, @RateLimit, ...)
│       ├── aop/                  # 5 切面
│       ├── security/             # JWT 过滤器、认证入口
│       ├── enums/                # 9 枚举
│       ├── exception/            # 自定义异常
│       ├── constant/             # 错误码、错误信息
│       └── util/                 # 工具类
│   └── src/main/resources/
│       ├── db/migration/         # Flyway 迁移脚本 (V1, V2, V4, V5)
│       └── application*.yml      # 多环境配置
│
├── oaiss-chain-frontend/         # Vue 3 前端
│   └── src/
│       ├── api/                  # 22 Axios API 模块
│       ├── views/                # 页面组件
│       │   ├── enterprise/       # 企业端 (16 页面)
│       │   ├── auditor/          # 审核员端 (3 页面)
│       │   ├── admin/            # 管理端 (6 页面)
│       │   └── third-party/      # 第三方监管 (1 页面)
│       ├── store/                # Pinia 状态管理
│       ├── router/               # 路由 + 角色守卫
│       ├── i18n/                 # 国际化 (中/英)
│       ├── components/           # 共享组件
│       ├── config/               # 菜单、图片配置
│       ├── types/                # TypeScript 类型定义
│       └── utils/                # 工具函数 (JWT, ECharts, 格式化)
│   └── tests/e2e/               # Playwright E2E 测试
│
├── oaiss-chain-ml-service/       # Python ML 微服务
│   └── app/
│       ├── services/             # 排放预测、企业推断、市场预测
│       ├── routers/              # FastAPI 路由
│       └── schemas/              # Pydantic 数据模型
│
├── oaiss-chain-chaincode/        # Hyperledger Fabric 链码
│   └── chaincode.go              # Go 智能合约 (碳报告 + 交易记录)
│
├── fabric-config/                # Fabric 网络配置与加密材料
├── scripts/                      # 测试/部署脚本
├── docs/                         # 项目文档
├── docker-compose.yml            # 主编排 (全栈 5 服务)
├── docker-compose.infra.yml      # 基础设施 (MySQL/Redis/MinIO)
└── docker-compose.fabric.yml     # Fabric 区块链网络
```

## 角色与功能

| 角色 | 首页路由 | 核心功能 |
|------|----------|----------|
| **企业** (ENTERPRISE) | `/enterprise/carbon/upload` | 碳报告上报、P2P/拍卖交易、碳币账户、信用评分、碳中和项目、AI 预测、区块链浏览、数字签名 |
| **审核员** (REVIEWER) | `/auditor/audit/list` | 碳报告审核、项目审核、审核历史 |
| **第三方监管** (THIRD_PARTY) | `/third-party/monitor` | 碳报告监控、统计数据、组织信息 |
| **管理员** (ADMIN) | `/admin/system/users` | 用户管理、系统配置、数据统计、准入证书、资格证管理 |

## 快速开始

### 环境要求

| 工具 | 版本 | 用途 |
|------|------|------|
| JDK | 17+ | 后端运行 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 18+ | 前端开发 |
| Docker & Docker Compose | 最新 | 基础设施 & 全栈部署 |
| Git | 最新 | 代码管理 |
| Python | 3.11+ | ML 服务（可选） |
| jq | 最新 | Fabric crypto 生成（可选） |

### 方式一：Docker Compose 一键启动

适合快速体验或演示环境。

```bash
# 1. 配置环境变量
cp .env.example .env
# 编辑 .env，设置 DB_PASSWORD, REDIS_PASSWORD, JWT_SECRET, MINIO_ACCESS_KEY, MINIO_SECRET_KEY

# 2. 启动所有服务
docker-compose up -d
```

服务端口：

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端 | http://localhost:5173 | Vue SPA |
| 后端 API | http://localhost:8080/api/v1 | REST API |
| Swagger UI | http://localhost:8080/api/v1/swagger-ui/index.html | API 文档 |
| MinIO Console | http://localhost:9003 | 对象存储管理 |
| MinIO API | http://localhost:9002 | 对象存储 API |
| ML Service | http://localhost:8001 | AI 预测服务 |
| 健康检查 | http://localhost:8080/api/v1/actuator/health | 后端状态 |
| Fabric Orderer | localhost:7050 | 排序节点（可选） |
| Fabric Peer | localhost:7051 | 背书节点（可选） |
| Fabric CA | localhost:7054 | 证书颁发（可选） |
| CouchDB | localhost:5984 | 状态数据库（可选） |

### 方式二：本地开发（推荐）

适合日常开发调试。后端在 IDEA 中运行，前端通过 Vite dev server 运行，基础设施用 Docker。

#### 第 1 步：启动基础设施（MySQL, Redis, MinIO, ML 服务）

```bash
cd "OAISS CHAIN"
docker-compose -f docker-compose.infra.yml up -d
```

| 容器 | 端口映射 | 默认密码 |
|------|----------|----------|
| oaiss-mysql | 127.0.0.1:**3306**→3306 | 见 `.env` 中 `DB_PASSWORD` |
| oaiss-redis | 127.0.0.1:**6379**→6379 | 见 `.env` 中 `REDIS_PASSWORD` |
| oaiss-minio | **9002**→9000 (API), **9003**→9001 (Console) | 见 `.env` 中 `MINIO_ACCESS_KEY` |
| oaiss-ml-service | 127.0.0.1:**8001**→8001 | 无 |

> **注意**: 如果本地已有 MySQL 占用 3306 端口，需先停止本地 MySQL 服务（Windows: `net stop MySQL`，需管理员权限）。
> MinIO Console 管理界面: http://localhost:9003

#### 第 2 步：IDEA 启动后端

1. IDEA → `File` → `Open` → 选择 `oaiss-chain-backend/pom.xml` → `Open as Project`
2. 等待 Maven 依赖下载完成
3. `File` → `Project Structure` → **Project SDK 设为 JDK 17**，Language Level 17
4. 确保 Lombok 插件已安装，并在 `Settings` → `Build` → `Compiler` → `Annotation Processors` 中启用
5. 配置运行参数：右键 `OaissChainApplication.java` → `Run`，然后 `Edit Configurations`：
   - **Active profiles**: `local`
   - **Environment variables**（可选，留空会使用 `application-local.yml` 中的默认值）
6. 点击运行 → 控制台出现 `Started OaissChainApplication` 即成功

后端可用的 Spring profiles：

| Profile | 用途 |
|---------|------|
| `local` | 本地开发，连接 Docker 基础设施（MySQL 3307），Fabric 关闭 |
| `dev` | 本地开发（无 Docker），MySQL 3306，Flyway 关闭，JPA ddl-auto=update |
| `local,fabric` | 本地开发 + 启用 Fabric 区块链连接 |
| `docker` | Docker Compose 全栈部署，服务间通过容器名通信 |
| `test` | 自动化测试，使用 Testcontainers |

#### 第 3 步：启动前端

```bash
cd oaiss-chain-frontend
npm install
npm run dev
```

浏览器打开 http://localhost:5173。

#### 第 4 步：登录验证

使用种子数据账户（所有密码均为 `admin123`）：

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | `admin` | `admin123` |
| 企业 | `enterprise001` / `enterprise002` / `enterprise003` | `admin123` |
| 审核员 | `reviewer001` | `admin123` |
| 第三方监管 | `thirdparty001` | `admin123` |

#### 第 5 步（可选）：启动 Fabric 区块链网络

```bash
# 前提：已生成加密材料
cd "OAISS CHAIN"
docker-compose -f docker-compose.fabric.yml up -d
```

验证：

```bash
docker-compose -f docker-compose.fabric.yml ps
# 应看到 5 个容器全部 Up: orderer, peer0, couchdb0, ca.org1, fabric-cli
```

启用 Fabric 后，IDEA `Run Configuration` 中将 Active profiles 改为 `default,fabric`。

> **注意**: `fabric` profile 需要所有 Fabric 容器正常运行，且加密材料已生成。否则后端启动失败。
> 区块链是**可选组件**，不启动不影响核心业务功能。

## Docker Compose 文件说明

项目包含三个 Compose 文件，分工不同：

| 文件 | 服务 | 用途 |
|------|------|------|
| `docker-compose.infra.yml` | MySQL, Redis, MinIO, ML Service | **本地开发**的基础设施，创建共享网络 `oaiss-network` |
| `docker-compose.fabric.yml` | Orderer, Peer, CouchDB, CA, CLI | Fabric 区块链网络（可选） |
| `docker-compose.yml` | MySQL, Redis, MinIO, Backend, Frontend, ML | **生产/演示**全栈部署 |

### 启动顺序

```
1. docker-compose -f docker-compose.infra.yml up -d     # 基础设施 + ML 服务（必须）
2. docker-compose -f docker-compose.fabric.yml up -d     # 区块链（可选）
3. IDEA 启动后端 / mvn spring-boot:run                    # 后端应用
4. cd oaiss-chain-frontend && npm run dev                 # 前端应用
```

### 健康检查

```bash
# 一键检查所有服务
bash ./scripts/health-check.sh

# 或手动逐项检查
curl http://localhost:8001/health                         # ML 服务
curl http://localhost:8080/api/v1/actuator/health         # 后端（需 Fabric 关闭时可能返回 DOWN）
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'         # 后端登录测试
curl http://localhost:9002/minio/health/live              # MinIO
docker exec oaiss-mysql mysql -uroot -p${DB_PASSWORD} -e "SELECT 1"   # MySQL
docker exec oaiss-redis redis-cli -a ${REDIS_PASSWORD} ping            # Redis
docker-compose -f docker-compose.fabric.yml ps            # Fabric 网络
```

## 区块链集成（Fabric）

Fabric 是**可选组件**，不启动不影响核心业务功能。启用后提供碳报告上链存证和交易记录不可篡改。

### 前置条件

- `fabric-samples` 仓库（含 `test-network`）
- `jq` 命令行 JSON 工具
- Docker（已安装）

### 部署步骤

```bash
# 1. 安装 fabric-samples（首次，需要几分钟）
# 可放在任意目录，例如 D:\fabric-samples
curl -sSLO https://raw.githubusercontent.com/hyperledger/fabric/main/scripts/install-fabric.sh
bash ./install-fabric.sh docker samples binary

# 2. 生成加密材料（需要 jq）
cd "OAISS CHAIN"
bash ./scripts/generate-fabric-crypto.sh /path/to/fabric-samples

# 3. 启动 Fabric 网络
docker-compose -f docker-compose.fabric.yml up -d
```

验证：

```bash
docker-compose -f docker-compose.fabric.yml ps
# 应看到 5 个容器全部 Up: orderer, peer0, couchdb0, ca.org1, fabric-cli
```

### 后端启用 Fabric

IDEA `Run Configuration` 中将 Active profiles 改为 `local,fabric`。

> **注意**: `fabric` profile 需要所有 Fabric 容器正常运行，且加密材料已生成。否则后端启动失败。

### 架构设计

系统通过 `BlockchainServicePort` 接口抽象区块链操作，提供两种实现：

| 实现 | Profile | 说明 |
|------|---------|------|
| `MockBlockchainService` | 默认 | 模拟区块链行为，适用于开发和测试 |
| `FabricBlockchainService` | `fabric` | 对接 Hyperledger Fabric，生产使用 |

Go 链码 `carbon-chaincode` 实现链上数据存储：
- `CreateCarbonReport` / `QueryReportHistory` — 碳报告上链与历史查询
- `CreateTradeRecord` / `VerifyReport` — 交易记录上链与报告验证

## AI/ML 服务

独立 FastAPI 微服务，提供三个预测域。不启动时核心业务正常，仅 AI 预测相关页面不可用。

| 服务 | 模型 | 端点 | 用途 |
|------|------|------|------|
| EmissionService | Prophet | `POST /predict/emission/forecast` | 碳排放时序预测 + 置信区间 |
| EnterpriseService | IsolationForest + XGBoost | `POST /api/v1/predict/enterprise/` | 企业合规风险推断 |
| MarketService | Prophet + XGBoost | `POST /predict/market/trend` | 碳市场趋势预测 |
| MarketService | Prophet + XGBoost | `POST /predict/market/price` | 碳价格预测 |
| MarketService | XGBoost | `POST /predict/market/supply-demand` | 供需量预测 |

后端通过 `MlServiceClient` (WebClient) 调用，配置 Resilience4j 熔断器（50% 失败阈值，30s 开路）。

API 文档：http://localhost:8001/docs

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_HOST` | `localhost` | MySQL 主机 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_USERNAME` | `root` | MySQL 用户名 |
| `DB_PASSWORD` | — | MySQL 密码（**必填**） |
| `REDIS_HOST` | `localhost` | Redis 主机 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_PASSWORD` | — | Redis 密码（**必填**） |
| `JWT_SECRET` | — | JWT 签名密钥（**必填**，至少 32 字符） |
| `JWT_EXPIRATION` | `3600000` | Token 有效期（毫秒，默认 1 小时） |
| `JWT_REFRESH_EXPIRATION` | `604800000` | Refresh Token 有效期（毫秒，默认 7 天） |
| `MINIO_ENDPOINT` | `http://localhost:9002` | MinIO 地址 |
| `MINIO_ACCESS_KEY` | — | MinIO Access Key |
| `MINIO_SECRET_KEY` | — | MinIO Secret Key |
| `MINIO_BUCKET` | `oaiss-chain` | MinIO 存储桶 |
| `ML_SERVICE_URL` | `http://localhost:8001` | ML 服务地址 |
| `SPRING_PROFILES_ACTIVE` | `default` | Spring Profile |

> 各 profile 的默认值定义在对应的 `application-{profile}.yml` 中，环境变量优先级高于配置文件。

## 测试

```bash
# 后端单元测试
cd oaiss-chain-backend && mvn test

# 后端集成测试（需 Docker）
cd oaiss-chain-backend && mvn verify

# 前端单元测试
cd oaiss-chain-frontend && npm run test

# 前端 E2E 测试
cd oaiss-chain-frontend && npm run test:e2e

# 前端 E2E 业务流程测试
cd oaiss-chain-frontend && npm run test:e2e:flow
```

## API 规范

- **基础路径**: `/api/v1`（通过 `server.servlet.context-path` 配置）
- **响应格式**: `ApiResponse<T>` — `{ code, message, data, meta }`
- **分页**: 前端发送 `pageNum`/`pageSize` → 拦截器转换为 `page`/`size`；后端返回 Spring Data `Page` → 前端转换为 `{ items, total, page, size, totalPages }`
- **认证**: JWT Bearer Token + 自动刷新（5 分钟内过期触发）
- **授权**: `@PreAuthorize` + `@RequirePermission` 双层控制

## 数据模型

系统包含 21+ 数据库表，核心实体：

| 实体 | 说明 |
|------|------|
| `User` | 用户（含 4 种角色类型） |
| `Enterprise` | 企业信息 |
| `CarbonReport` | 碳排放报告（状态机：草稿→待审→通过/驳回） |
| `Transaction` | 碳交易记录 |
| `AuctionOrder` / `MatchingResult` | 双向拍卖订单与撮合结果 |
| `CarbonCoinAccount` / `CarbonCoinTransaction` | 碳币账户与流水 |
| `CreditScore` / `CreditEvent` | 信用评分与事件 |
| `CarbonNeutralProject` | 碳中和项目 |
| `EmissionRating` | 排放评级 |
| `RsaKeyPair` | RSA 密钥对 |
| `OperationLog` | 操作审计日志 |

所有实体继承 `BaseEntity`（软删除 `deleted` 字段 + 审计字段 `createdAt`/`updatedAt`）。

数据库迁移通过 Flyway 管理（V1 初始化、V2 种子数据、V4 企业准入、V5 乐观锁），脚本位于 `oaiss-chain-backend/src/main/resources/db/migration/`。

## 横切关注点 (AOP)

| 注解 | 切面 | 功能 |
|------|------|------|
| `@AuditLog` | AuditLogAspect | 操作审计日志记录 |
| `@RateLimit` | RateLimitAspect | API 请求限流 |
| `@DataIsolation` | DataIsolationAspect | 租户数据隔离 |
| `@DistributedLock` | DistributedLockAspect | Redis 分布式锁（支持 SpEL） |
| `@RequirePermission` | PermissionAspect | 细粒度权限检查 |

## 常见问题

### 后端启动后 Banner 之后就无输出

`logback-spring.xml` 需要为 `local` profile 配置日志输出。确保 `<springProfile>` 的 name 属性包含 `local`。

### Swagger UI 访问被拦截返回 401

Swagger 路径受 JWT 安全过滤器保护。本地开发时可先通过前端登录获取 Token，或通过 `/api/v1/auth/login` 接口获取。Swagger UI 正确路径为 `/api/v1/swagger-ui/index.html`。

### Fabric CA 启动报版本冲突

```
Error: Configuration file version 'v1.5.19' is higher than server version 'v1.5.17'
```

`docker-compose.fabric.yml` 中 CA 镜像版本需要与生成 crypto 时使用的版本匹配。确保使用 `fabric-ca:1.5.19` 或更高版本。

### 后端 fabric profile 启动报 NoClassDefFoundError: protobuf

`fabric-gateway:1.7.1` 需要 protobuf 4.x，需在 `pom.xml` 中显式声明 `protobuf-java` 4.x 版本。

### MySQL 连接失败

- 确认 Docker 容器 `oaiss-mysql` 正在运行：`docker ps | grep mysql`
- 确保本地没有 MySQL 占用 3306 端口：`netstat -ano | findstr ":3306"`（Windows）或 `lsof -i :3306`（macOS/Linux）
- 如有冲突，停止本地 MySQL 服务：Windows `net stop MySQL`（需管理员），Linux `sudo systemctl stop mysql`

### JWT_SECRET 报错

JWT 密钥长度必须至少 256-bit（32+ 字符）。

## 开发路线图

| 里程碑 | 阶段 | 状态 |
|--------|------|------|
| **v1.0** 手工测试 | Phase 1-6 (环境/碳报告/交易/项目/支撑/边界) | 已发布 2026-05-13 |
| **v1.1.0** 需求对齐 | Phase 7-12 (AI/公式/区块链/准入/前端覆盖/E2E) | 已发布 2026-05-18 |
| **v2.0** 安全与性能 | Phase 13-15 (并发安全/性能优化/DevOps) | 进行中 |

## License

Private — All rights reserved.

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
│  │  20 Controllers → 29 Services → 21 Repositories      │   │
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
│       ├── entity/               # 22 JPA 实体 (21 业务 + BaseEntity)
│       ├── dto/                  # 请求/响应 DTO
│       ├── config/               # 19 配置类 (Security, Redis, MinIO, Fabric, ML...)
│       ├── annotation/           # 5 自定义注解 (@AuditLog, @RateLimit, ...)
│       ├── aop/                  # 5 切面
│       ├── security/             # JWT 过滤器、认证入口
│       ├── enums/                # 9 枚举
│       ├── exception/            # 自定义异常
│       ├── constant/             # 错误码、错误信息
│       └── util/                 # 工具类
│   └── src/main/resources/
│       ├── db/migration/         # Flyway 迁移脚本 (V1-V8, V3 缺失)
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
├── docker-compose.yml            # 全栈容器编排（本地/演示）
├── docker-compose.prod.yml       # 生产容器编排（仅应用服务）
├── docker-compose.infra.yml      # 基础设施 (MySQL/Redis/MinIO/ML Service)
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
docker compose up -d
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
docker compose -f docker-compose.infra.yml up -d
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
| `local` | 本地开发，连接 Docker 基础设施（MySQL 3306），Fabric 关闭 |
| `dev` | 本地开发（无 Docker），MySQL 3306，Flyway 关闭，JPA ddl-auto=update |
| `local,fabric` | 本地开发 + 启用 Fabric 区块链连接 |
| `docker` | Docker Compose 全栈部署，服务间通过容器名通信 |
| `test` | 自动化测试，使用 Testcontainers |

#### 第 3 步：启动前端（手动路径）

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

#### 第 5 步（可选）：启动 Fabric 区块链网络（手动路径）

```bash
# 前提：已生成加密材料
cd "OAISS CHAIN"
docker compose -f docker-compose.fabric.yml up -d
```

验证：

```bash
docker compose -f docker-compose.fabric.yml ps
# 应看到 5 个容器全部 Up: orderer, peer0, couchdb0, ca.org1, fabric-cli
```

启用 Fabric 后，如仍使用 IDEA 手动启动后端，请将 `Run Configuration` 中的 Active profiles 改为 `local,fabric`。

> **注意**: `fabric` profile 需要所有 Fabric 容器正常运行，且加密材料已生成。否则后端启动失败。
> 区块链是**可选组件**，不启动不影响核心业务功能。

## Docker Compose 文件说明

项目包含三个 Compose 文件，分工不同：

| 文件 | 服务 | 用途 |
|------|------|------|
| `docker-compose.infra.yml` | MySQL, Redis, MinIO, ML Service | **本地开发**的基础设施，创建共享网络 `oaiss-network` |
| `docker-compose.fabric.yml` | Orderer, Peer, CouchDB, CA, CLI | Fabric 区块链网络（可选） |
| `docker-compose.yml` | MySQL, Redis, MinIO, Backend, Frontend, ML | **本地集成/演示**全栈部署 |
| `docker-compose.prod.yml` | Frontend, Backend, ML | **生产部署**应用层编排；数据库、Redis、对象存储应使用托管或私有基础设施 |

### 启动顺序

以下为手动启动路径；日常本地开发优先使用下一节的一键脚本。

```
1. docker compose -f docker-compose.infra.yml up -d     # 基础设施 + ML 服务（必须）
2. docker compose -f docker-compose.fabric.yml up -d     # 区块链（可选）
3. scripts/start-backend.bat 或 ./scripts/start-backend.sh  # 后端应用
4. cd oaiss-chain-frontend && npm run dev                 # 前端应用
```

### 一键启动脚本

项目提供统一启动/停止脚本，自动处理依赖等待和健康检查：

**Linux / macOS：**

```bash
# 启动基础服务 + 后端 + 前端（推荐日常开发）
./scripts/start-all.sh

# 启动全部服务（含 Fabric 区块链）
./scripts/start-all.sh --with-fabric

# 仅启动基础设施
./scripts/start-all.sh --infra-only

# 跳过特定服务
./scripts/start-all.sh --skip-frontend --skip-backend

# 停止所有服务
./scripts/stop-all.sh

# 停止所有服务（含 Fabric）
./scripts/stop-all.sh --with-fabric
```

**Windows：**

```bat
REM 启动基础服务 + 后端 + 前端（后端和前端在独立窗口运行）
scripts\start-all.bat

REM 启动全部服务（含 Fabric 区块链）
scripts\start-all.bat --with-fabric

REM 仅启动基础设施
scripts\start-all.bat --infra-only

REM 停止所有服务
scripts\stop-all.bat
```

> 脚本特性：
> - 加载 `.env` 环境变量
> - 自动轮询 Docker 容器健康状态（`docker inspect`），等待 MySQL/Redis/MinIO 就绪后再启动应用
> - 后端使用 `local` profile，Fabric 使用 `fabric` profile
> - Windows 脚本中后端和前端各在独立 `cmd` 窗口运行，关闭窗口即停止进程

> 推荐做法：
> - 本地开发优先使用 `scripts/start-all.bat --with-fabric` 或 `./scripts/start-all.sh --with-fabric`
> - 只想单独启动后端时，使用 `scripts/start-backend.bat` 或 `./scripts/start-backend.sh`
> - 停止单独后端时，使用 `scripts/stop-backend.bat` 或 `./scripts/stop-backend.sh`
> - 不要直接在未注入 `.env` 的终端里运行裸 `mvn spring-boot:run`
> - `RSA_KEK`、`JWT_SECRET`、`DB_PASSWORD` 等敏感变量依赖 `.env` 或 IDE Run Configuration 注入
> - 生产环境不要复用 `docker-compose.yml`；请使用 `docker-compose.prod.yml` 和 `prod` / `prod,fabric` profile

> 最简入口：
> - 全栈本地联调：`scripts\start-all.bat --with-fabric` / `./scripts/start-all.sh --with-fabric`
> - 单独后端联调：`scripts\start-backend.bat --with-fabric` / `./scripts/start-backend.sh --with-fabric`

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
docker compose -f docker-compose.fabric.yml ps            # Fabric 网络
```

### 生产部署入口

生产环境与本地 `local` / `local,fabric` 联调是两条不同路径：

- 本地联调：优先使用 `scripts/start-all.bat --with-fabric` / `./scripts/start-all.sh --with-fabric`
- 生产部署：使用 `docker-compose.prod.yml`，通过 `--env-file` 指向生产环境文件，并设置 `SPRING_PROFILES_ACTIVE=prod` 或 `prod,fabric`

更完整的生产发布约束、回滚策略和验收清单见
`docs/production-readiness.md`。

实际部署时的操作顺序、staging/prod 差异、上线后验收与回滚步骤见
`docs/deployment-runbook.md`。

最终收口所需的本地 `local,fabric` 探针结果、串行执行建议和发布前人工检查项见
`docs/final-acceptance-checklist.md`。

> 生产环境如果仓库根目录存在本地开发 `.env`，请额外设置 `COMPOSE_DISABLE_ENV_FILE=1`，避免 Docker Compose 隐式加载本地变量并污染生产配置。
> 在 Windows 本地终端里，如果当前进程已经注入过 `DB_PASSWORD`、`JWT_SECRET`、`SPRING_PROFILES_ACTIVE` 等开发变量，优先使用 `scripts/prod-compose.ps1` 包装脚本执行生产 compose 命令。
> Linux / macOS 终端请使用 `scripts/prod-compose.sh --env-file /secure/path/oaiss-chain.env ...`，它会执行同样的清理动作。
> 如果是在 Windows 的 WSL 里调用 `scripts/prod-compose.sh`，还需要先给该 WSL 发行版启用 Docker Desktop integration。

## 区块链集成（Fabric）

Fabric 是**可选组件**，不启动不影响核心业务功能。启用后提供碳报告上链存证和交易记录不可篡改。

### 状态数据库

Fabric Peer 默认使用 **LevelDB**（内嵌式，无需外部依赖）。如需使用 CouchDB（支持富查询），修改 `docker-compose.fabric.yml` 中 Peer 的环境变量：

```yaml
- CORE_LEDGER_STATE_STATEDATABASE=CouchDB
- CORE_LEDGER_STATE_COUCHDBCONFIG_COUCHDBADDRESS=couchdb0:5984
- CORE_LEDGER_STATE_COUCHDBCONFIG_USERNAME=admin
- CORE_LEDGER_STATE_COUCHDBCONFIG_PASSWORD=adminpw
```

> **注意**: CouchDB 单节点部署需配置 `cluster/n=1` 和 `cluster/q=1`，否则 Peer 启动时报 `unexpected end of JSON input` 错误。配置文件位于 `fabric-config/couchdb/docker.ini`。

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
docker compose -f docker-compose.fabric.yml up -d
```

验证：

```bash
docker compose -f docker-compose.fabric.yml ps
# 应看到 5 个容器全部 Up: orderer, peer0, couchdb0, ca.org1, fabric-cli
```

### 后端启用 Fabric

IDEA `Run Configuration` 中将 Active profiles 改为 `local,fabric`。

> **注意**: `fabric` profile 需要所有 Fabric 容器正常运行，且加密材料已生成。否则后端启动失败。
> 同时需要注入 `.env` 中的 `RSA_KEK`、`JWT_SECRET`、`DB_PASSWORD` 等环境变量；否则后端会在启动阶段因密钥加密器初始化失败而退出。

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
| `RSA_KEK` | — | RSA 私钥 AES-256-GCM 加密密钥（**必填**，需为 Base64 编码的 32 字节密钥） |
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

<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-19 -->

# OAISS Chain Backend

## Purpose

Spring Boot 3.2.5 分层架构后端，提供碳核算、碳交易、信用评分、区块链存证等 REST API。
Package: `com.oaiss.chain` | Entry: `OaissChainApplication.java`

---

## Key Files

| File | Purpose |
|------|---------|
| `pom.xml` | Maven 构建配置 (Spring Boot 3.2.5, Java 17, 依赖管理) |
| `Dockerfile` | 多阶段 Docker 构建 (Maven build + JRE runtime) |
| `../docker-compose.yml` | 全栈编排 (MySQL, Redis, MinIO, backend, frontend) |
| `../.env.example` | 环境变量模板 (DB_PASSWORD, REDIS_PASSWORD, JWT_SECRET, MINIO_*) |
| `API_DOCUMENT.md` | OpenAPI/Swagger API 文档 |

## Subdirectories

| Directory | Description | Child Guide |
|-----------|-------------|-------------|
| `src/` | Java 源码 (main + test) | — |
| `docs/` | 测试报告与项目文档 | — |
| `monitoring/` | Prometheus + Grafana + Alertmanager 配置 | — |
| `tools/` | 诊断工具 (Arthas, JFR, profiler 脚本) | — |
| `../.github/workflows/` | CI/CD GitHub Actions 流水线 | — |
| `logs/` | 应用运行日志 (git-ignored) | — |

---

## Structure

```
src/main/java/com/oaiss/chain/
├── annotation/     # 自定义注解 (@AuditLog, @RateLimit, @DistributedLock)
├── aop/            # AOP 切面 (审计日志、限流、分布式锁)
├── config/         # 配置类 (Security, Redis, MinIO, Swagger, Cache)
├── constant/       # 常量定义
├── controller/     # REST 控制器 (20 个)
├── dto/            # 数据传输对象 (44 个)
├── entity/         # JPA 实体 (23 个，含 BaseEntity)
├── enums/          # 枚举类型 (9 个)
├── exception/      # 异常处理 (7 个，含全局异常处理器)
├── repository/     # Spring Data 仓库 (22 个)
├── security/       # 安全组件 (6 个: JWT 过滤器、TokenProvider、Handler 等)
├── service/        # 业务服务 (31 个: 28 主包 + 3 ml/ 子包)
└── util/           # 工具类 (4 个)
```

---

## For AI Agents

### Working

| Task | Location |
|------|----------|
| 认证登录 | `controller/AuthController.java` + `service/AuthService.java` |
| 碳核算 | `controller/CarbonController.java` + `service/CarbonService.java` |
| 碳交易 | `controller/TradeController.java` + `service/TradeService.java` |
| 双向拍卖 | `controller/DoubleAuctionController.java` + `service/DoubleAuctionService.java` |
| 碳币账户 | `controller/CarbonCoinController.java` + `service/CarbonCoinService.java` |
| 信用评分 | `controller/CreditScoreController.java` + `service/CreditScoreService.java` |
| 数字签名 | `controller/DigitalSignatureController.java` + `service/DigitalSignatureService.java` |
| 区块链 | `controller/BlockchainController.java` + `service/FabricBlockchainService.java` |
| 文件上传 | `controller/FileController.java` + `service/MinioService.java` |
| 用户管理 | `controller/UserController.java` + `service/UserService.java` |
| 排放评级 | `controller/EmissionController.java` + `service/EmissionRatingService.java` |
| 碳中和项目 | `controller/CarbonNeutralProjectController.java` + `service/CarbonNeutralProjectService.java` |
| 企业管理 | `controller/EnterpriseController.java` + `service/EnterpriseService.java` |
| 审核员管理 | `controller/ReviewerController.java` + `service/ReviewerService.java` |
| 第三方监管 | `controller/ThirdPartyController.java` + `service/ThirdPartyService.java` |
| 管理后台 | `controller/AdminController.java` + `service/UserService.java` |
| 搜索 | `controller/SearchController.java` + `service/SearchService.java` |
| 验证码 | `controller/CaptchaController.java` + `service/CaptchaService.java` |
| ML 推理 | `controller/EnterpriseInferenceController.java` + `service/ml/EnterpriseInferenceService.java` |
| ML 预测 | `controller/MarketPredictionController.java` + `service/ml/MarketPredictionService.java` |

### Key Entities

| Entity | Table | Purpose |
|--------|-------|---------|
| `User` | user | 用户账户 |
| `Enterprise` | enterprise | 企业信息 |
| `CarbonReport` | carbon_report | 碳核算报告 |
| `Transaction` | trade_transaction | 交易流水 |
| `AuctionOrder` | auction_order | 拍卖订单 |
| `MatchingResult` | matching_result | 撮合结果 |
| `CarbonCoinAccount` | carbon_coin_account | 碳币账户 |
| `CarbonCoinTransaction` | carbon_coin_transaction | 碳币交易流水 |
| `CreditScore` | credit_score | 信用评分 |
| `CreditEvent` | credit_event | 信用事件 |
| `EmissionRating` | emission_rating | 排放评级 |
| `CarbonNeutralProject` | carbon_neutral_project | 碳中和项目 |
| `RsaKeyPair` | rsa_key_pair | RSA 密钥对 |
| `Reviewer` | reviewer | 审核员 |
| `ReviewerQualification` | reviewer_qualification | 审核员资质 |
| `Authenticator` | authenticator | 认证器 |
| `ThirdPartyOrg` | third_party_org | 第三方组织 |
| `EnterpriseAdmission` | enterprise_admission | 企业准入 |
| `OperationLog` | operation_log | 操作日志 |
| `UserTypeList` | user_type_list | 用户类型列表 |
| `AccountPermissionList` | account_permission_list | 账户权限列表 |
| `EntryPermission` | entry_permission | 入场权限 |

### API Endpoints

```
/api/v1/auth/login              # 登录
/api/v1/auth/register           # 注册
/api/v1/carbon/reports          # 碳核算报告
/api/v1/trade/orders            # 交易订单
/api/v1/auction/orders          # 拍卖订单
/api/v1/coin/accounts           # 碳币账户
/api/v1/credit/scores           # 信用评分
/api/v1/blockchain/tx           # 区块链交易
/api/v1/files/upload            # 文件上传
/api/v1/admin/users             # 用户管理 (管理员)
/api/v1/emission/ratings        # 排放评级
/api/v1/carbon-neutral/projects # 碳中和项目
/api/v1/enterprise/profile      # 企业信息
/api/v1/reviewer/reports        # 审核员审核
/api/v1/third-party/monitor     # 第三方监管
/api/v1/inference/enterprise    # ML 企业推理
/api/v1/prediction/market       # ML 市场预测
/api/v1/search                  # 全文搜索
/api/v1/captcha                 # 验证码
```

### Testing

```bash
mvn test                      # 单元测试 (*Test.java)
mvn verify                    # 集成测试 (*IntegrationTest.java)
mvn test -Dtest=AuthServiceTest  # 单个测试类
```

- **Base**: `BaseIntegrationTest.java` 提供 Testcontainers
- **Coverage**: JaCoCo 90% 目标，当前 ~25%
- **Test files**: 83 个 (18 controller, 24 service, 8 repository, 4 security, 6 aop, 4 config, 6 exception, 4 dto, 1 entity, 2 util, 1 integration, 2 root)
- **Pattern**: `methodName_WhenCondition_ShouldExpectedBehavior()`

### Patterns

| Layer | Convention |
|-------|-----------|
| **Controller** | 参数校验用 `@Valid` + DTO，响应用 `ApiResponse<T>` |
| **Service** | 业务逻辑 + 事务 `@Transactional`，调用 Repository |
| **Repository** | 继承 `JpaRepository`，自定义查询用 `@Query` |
| **Entity** | `@Entity` + `@Table`，时间字段 `created_at/updated_at` |
| **DTO** | MapStruct 映射，Lombok `@Data/@Builder` |
| **Exception** | `BusinessException` + 错误码枚举 |

### Configuration

| File | Purpose |
|------|---------|
| `application.yml` | 主配置 (端口 8080, context `/api/v1`) |
| `application-docker.yml` | Docker 环境配置 |
| `application-test.yml` | 测试环境 (H2 数据库) |
| `logback-spring.xml` | 日志配置 (JSON + ELK) |

---

## Dependencies

| Dependency | Version | Purpose |
|-----------|---------|---------|
| Spring Boot | 3.2.5 | 核心框架 |
| Spring Data JPA | (managed) | 数据访问层 |
| Spring Security | (managed) | 认证/授权 |
| MySQL Connector | (managed) | 数据库驱动 |
| Lettuce (Redis) | (managed) | 缓存/分布式锁 |
| MinIO SDK | (managed) | 对象存储 |
| jjwt | 0.12.5 | JWT Token 生成/验证 |
| Flyway | (managed) | 数据库迁移 |
| SpringDoc OpenAPI | 2.5 | API 文档 |
| JaCoCo | (plugin) | 代码覆盖率 |

### Runtime Notes

- JWT 过期: Access 1h, Refresh 7d
- MinIO 最大文件: 100MB
- Redis: Lettuce 连接池 8 max
- HikariCP: 5-20 连接池

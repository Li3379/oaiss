# OAISS Chain Backend - Spring Boot 应用

**Package:** `com.oaiss.chain`
**Entry:** `OaissChainApplication.java`

## OVERVIEW

双碳链动系统后端，Spring Boot 3.2.5 分层架构，提供碳核算、碳交易、信用评分、区块链存证等 API。

## STRUCTURE

```
src/main/java/com/oaiss/chain/
├── annotation/     # 自定义注解 (@AuditLog, @RateLimit, @DistributedLock)
├── aop/            # AOP 切面 (审计日志、限流、分布式锁)
├── config/         # 配置类 (Security, Redis, MinIO, Swagger, Cache)
├── constant/       # 常量定义
├── controller/     # REST 控制器 (16 个)
├── dto/            # 数据传输对象 (34 个)
├── entity/         # JPA 实体 (22 个)
├── enums/          # 枚举类型
├── exception/      # 异常处理 (全局异常处理器)
├── repository/     # Spring Data 仓库 (21 个)
├── security/       # 安全组件 (JWT 过滤器、TokenProvider)
├── service/        # 业务服务 (19 个)
└── util/           # 工具类
```

## WHERE TO LOOK

| Task | Location |
|------|----------|
| 认证登录 | `controller/AuthController.java` + `service/AuthService.java` |
| 碳核算 | `controller/CarbonController.java` + `service/CarbonService.java` |
| 碳交易 | `controller/TradeController.java` + `service/TradeService.java` |
| 双向拍卖 | `controller/DoubleAuctionController.java` + `service/DoubleAuctionService.java` |
| 碳币账户 | `controller/CarbonCoinController.java` + `service/CarbonCoinService.java` |
| 信用评分 | `controller/CreditScoreController.java` + `service/CreditScoreService.java` |
| 数字签名 | `controller/DigitalSignatureController.java` + `service/DigitalSignatureService.java` |
| 区块链 | `controller/BlockchainController.java` + `service/BlockchainService.java` |
| 文件上传 | `controller/FileController.java` + `service/MinioService.java` |
| 用户管理 | `controller/UserController.java` + `service/UserService.java` |

## KEY ENTITIES

| Entity | Table | Purpose |
|--------|-------|---------|
| `User` | user | 用户账户 |
| `Enterprise` | enterprise | 企业信息 |
| `CarbonReport` | carbon_report | 碳核算报告 |
| `Transaction` | trade_transaction | 交易流水 |
| `AuctionOrder` | auction_order | 拍卖订单 |
| `CarbonCoinAccount` | carbon_coin_account | 碳币账户 |
| `CreditScore` | credit_score | 信用评分 |
| `RsaKeyPair` | rsa_key_pair | RSA 密钥对 |

## API ENDPOINTS

```
/api/v1/auth/login              # 登录
/api/v1/auth/register           # 注册
/api/v1/carbon/reports          # 碳核算报告
/api/v1/trade/orders            # 交易订单
/api/v1/auction/orders          # 拍卖订单
/api/v1/coin/accounts           # 碳币账户
/api/v1/credit/scores           # 信用评分
/api/v1/blockchain/tx           # 区块链交易
/api/v1/files/upload           # 文件上传
/api/v1/admin/users            # 用户管理 (管理员)
```

## CONVENTIONS

- **Controller**: 参数校验用 `@Valid` + DTO，响应用 `ApiResponse<T>`
- **Service**: 业务逻辑 + 事务 `@Transactional`，调用 Repository
- **Repository**: 继承 `JpaRepository`，自定义查询用 `@Query`
- **Entity**: `@Entity` + `@Table`，时间字段 `created_at/updated_at`
- **DTO**: MapStruct 映射，Lombok `@Data/@Builder`
- **异常**: `BusinessException` + 错误码枚举

## TESTING

```bash
mvn test                      # 单元测试 (*Test.java)
mvn verify                    # 集成测试 (*IntegrationTest.java)
mvn test -Dtest=AuthServiceTest  # 单个测试类
```

- **Base**: `BaseIntegrationTest.java` 提供 Testcontainers
- **Coverage**: JaCoCo 90% 目标，当前 25%
- **Pattern**: `methodName_WhenCondition_ShouldExpectedBehavior()`

## CONFIGURATION

| File | Purpose |
|------|---------|
| `application.yml` | 主配置 (端口 8080, context `/api/v1`) |
| `application-docker.yml` | Docker 环境配置 |
| `application-test.yml` | 测试环境 (H2 数据库) |
| `logback-spring.xml` | 日志配置 (JSON + ELK) |

## NOTES

- JWT 过期: Access 1h, Refresh 7d
- MinIO 最大文件: 100MB
- Redis: Lettuce 连接池 8 max
- HikariCP: 5-20 连接池
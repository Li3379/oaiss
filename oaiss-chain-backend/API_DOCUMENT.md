# 双碳链动系统 API 文档

> 基于区块链的可信碳核算与交易平台后端API文档
> 
> 版本: 1.0.0
> 
> 生成日期: 2026-04-26

---

## 目录

1. [概述](#概述)
2. [API文档生成方式](#api文档生成方式)
3. [认证说明](#认证说明)
4. [通用响应格式](#通用响应格式)
5. [API接口列表](#api接口列表)
6. [错误码说明](#错误码说明)

---

## 概述

双碳链动系统是一个基于区块链技术的可信碳核算与交易平台，提供碳报告管理、碳配额交易、信誉评分、碳中和项目等核心功能。

**技术栈**: Spring Boot 3.2.5 + springdoc-openapi 2.5.0 + MySQL + Redis + MinIO + Hyperledger Fabric

**API基础URL**: `http://localhost:8080/api/v1`

---

## API文档生成方式

项目已集成 **springdoc-openapi** (OpenAPI 3.0规范)，支持多种文档生成和访问方式：

### 方式一：Swagger UI（推荐）

启动应用后访问交互式API文档：

```
http://localhost:8080/api/v1/swagger-ui.html
```

**功能特点**：
- 可视化接口文档
- 在线测试API
- 查看请求/响应模型
- 支持JWT认证测试

### 方式二：OpenAPI JSON/YAML

获取原始OpenAPI规范文件：

```bash
# JSON格式
curl http://localhost:8080/api/v1/v1/api-docs -o openapi.json

# YAML格式（如需）
curl http://localhost:8080/api/v1/v1/api-docs.yaml -o openapi.yaml
```

### 方式三：Maven插件生成静态文档

使用 `openapi-generator-maven-plugin` 生成静态HTML文档：

```xml
<!-- 添加到 pom.xml -->
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>6.6.0</version>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <inputSpec>${project.basedir}/openapi.json</inputSpec>
                <generatorName>html2</generatorName>
                <outputDir>${project.basedir}/docs/api</outputDir>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 方式四：导出Postman Collection

1. 访问 Swagger UI
2. 复制 OpenAPI JSON URL: `http://localhost:8080/api/v1/v1/api-docs`
3. Postman → Import → Link → 粘贴URL

### 配置说明

**application.yml 配置**：
```yaml
springdoc:
  api-docs:
    path: /v1/api-docs          # OpenAPI JSON路径
  swagger-ui:
    path: /swagger-ui.html       # Swagger UI路径
    tags-sorter: alpha           # 按字母排序标签
```

**SwaggerConfig.java 配置**：
- API标题、描述、版本
- JWT Bearer认证配置
- 联系方式和许可证信息

---

## 认证说明

系统使用JWT Bearer Token进行身份认证。

### 获取Token

```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "your_username",
  "password": "your_password",
  "captchaKey": "xxx",
  "captchaCode": "1234"
}
```

### 使用Token

在请求头中添加：
```
Authorization: Bearer <access_token>
```

### Token刷新

使用RefreshToken获取新Token：
```
Refresh-Token: <refresh_token>
```

### 用户角色

| 角色代码 | 角色名称 | 权限说明 |
|---------|---------|---------|
| ENTERPRISE | 企业用户 | 提交碳报告、参与交易、管理项目 |
| REVIEWER | 审核员 | 审核碳报告、扣除信誉分 |
| AUTHENTICATOR | 认证员 | 区块链认证操作 |
| THIRD_PARTY | 第三方机构 | 监管查询、数据统计 |
| ADMIN | 管理员 | 系统管理、用户管理 |

---

## 通用响应格式

所有接口返回统一的JSON格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { },
  "meta": {
    "requestId": "req_1234567890_1",
    "timestamp": "2026-04-26T10:30:00",
    "pagination": {
      "page": 1,
      "size": 10,
      "total": 100,
      "totalPages": 10
    }
  }
}
```

**字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 响应状态码，200表示成功 |
| message | String | 响应消息 |
| data | Object | 响应数据 |
| meta.requestId | String | 请求ID，用于链路追踪 |
| meta.timestamp | DateTime | 响应时间戳 |
| meta.pagination | Object | 分页信息（列表接口） |

---

## API接口列表

### 01. 认证管理 `/auth`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| POST | `/auth/login` | 用户登录 | ❌ |
| POST | `/auth/register` | 用户注册 | ❌ |
| POST | `/auth/captcha` | 获取图形验证码 | ❌ |
| POST | `/auth/refresh` | 刷新访问令牌 | ❌ |
| GET | `/auth/check-ip` | IP访问检查 | ❌ |
| GET | `/auth/me` | 获取当前用户信息 | ✅ |
| POST | `/auth/logout` | 用户登出 | ✅ |
| PUT | `/auth/password` | 修改密码 | ✅ |

---

### 02. 用户中心 `/user`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| GET | `/user/profile` | 获取当前用户资料 | ✅ |
| GET | `/user/{userId}` | 根据ID获取用户信息 | ✅ |
| PUT | `/user/profile` | 更新用户资料 | ✅ |
| PUT | `/user/password` | 修改密码 | ✅ |
| GET | `/user/check-username` | 检查用户名可用性 | ❌ |
| GET | `/user/check-email` | 检查邮箱可用性 | ❌ |

---

### 03. 碳核算管理 `/carbon`

| 方法 | 路径 | 说明 | 认证 | 权限 |
|------|------|------|:----:|------|
| POST | `/carbon/reports` | 创建碳报告 | ✅ | ENTERPRISE |
| POST | `/carbon/reports/{reportId}/submit` | 提交碳报告 | ✅ | ENTERPRISE |
| GET | `/carbon/reports/{reportId}` | 获取报告详情 | ✅ | - |
| GET | `/carbon/reports` | 查询报告列表 | ✅ | ADMIN/REVIEWER/THIRD_PARTY |
| GET | `/carbon/my-reports` | 查询我的报告 | ✅ | ENTERPRISE |
| DELETE | `/carbon/reports/{reportId}` | 删除报告 | ✅ | ENTERPRISE |
| POST | `/carbon/review` | 审核碳报告 | ✅ | REVIEWER |

---

### 04. 碳交易管理 `/trade`

| 方法 | 路径 | 说明 | 认证 | 权限 |
|------|------|------|:----:|------|
| POST | `/trade/p2p` | 创建P2P交易 | ✅ | ENTERPRISE |
| POST | `/trade/auction` | 创建拍卖挂单 | ✅ | ENTERPRISE |
| POST | `/trade/{tradeId}/confirm` | 确认交易 | ✅ | ENTERPRISE/ADMIN |
| POST | `/trade/{tradeId}/cancel` | 取消交易 | ✅ | ENTERPRISE |
| GET | `/trade/{tradeId}` | 获取交易详情 | ✅ | - |
| GET | `/trade/list` | 查询交易列表 | ✅ | ADMIN/REVIEWER/THIRD_PARTY |
| GET | `/trade/my-trades` | 查询我的交易 | ✅ | ENTERPRISE |

---

### 05. 信誉评分管理 `/credit`

| 方法 | 路径 | 说明 | 认证 | 权限 |
|------|------|------|:----:|------|
| GET | `/credit/{enterpriseId}` | 查询企业信誉分 | ✅ | - |
| GET | `/credit/{enterpriseId}/history` | 查询信誉事件历史 | ✅ | - |
| POST | `/credit/deduct` | 扣除信誉分 | ✅ | ADMIN/REVIEWER |
| POST | `/credit/bonus` | 添加信誉奖励分 | ✅ | ADMIN |
| POST | `/credit/evaluate/{enterpriseId}` | 重新评估信誉等级 | ✅ | ADMIN |
| GET | `/credit/restricted` | 查询被限制交易企业 | ✅ | ADMIN |
| GET | `/credit/frozen` | 查询被冻结企业 | ✅ | ADMIN |
| GET | `/credit/check-permission/{enterpriseId}` | 检查交易权限 | ✅ | - |

---

### 06. 验证码管理 `/captcha`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| GET | `/captcha/generate` | 生成图形验证码 | ❌ |
| POST | `/captcha/verify` | 验证验证码 | ❌ |
| POST | `/captcha/sms/send` | 发送短信验证码 | ❌ |
| POST | `/captcha/email/send` | 发送邮箱验证码 | ❌ |

---

### 07. 文件管理 `/v1/file`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| POST | `/v1/file/upload` | 上传单个文件 | ✅ |
| POST | `/v1/file/upload/batch` | 批量上传文件 | ✅ |
| GET | `/v1/file/download` | 下载文件 | ✅ |
| DELETE | `/v1/file` | 删除文件 | ✅ |
| DELETE | `/v1/file/batch` | 批量删除文件 | ✅ |
| GET | `/v1/file/info` | 获取文件信息 | ✅ |
| GET | `/v1/file/exists` | 检查文件是否存在 | ✅ |
| GET | `/v1/file/presigned-url` | 获取预签名URL | ✅ |
| GET | `/v1/file/presigned-upload-url` | 获取上传预签名URL | ✅ |
| GET | `/v1/file/list` | 列出文件 | ✅ |
| POST | `/v1/file/copy` | 复制文件 | ✅ |

---

### 08. 数字签名 `/signature`

| 方法 | 路径 | 说明 | 认证 | 权限 |
|------|------|------|:----:|------|
| POST | `/signature/keypair/generate` | 生成RSA密钥对 | ✅ | ENTERPRISE/REVIEWER/THIRD_PARTY |
| GET | `/signature/keypair` | 获取密钥对信息 | ✅ | ENTERPRISE/REVIEWER/THIRD_PARTY |
| DELETE | `/signature/keypair` | 撤销密钥对 | ✅ | ENTERPRISE/REVIEWER/THIRD_PARTY |
| POST | `/signature/sign` | 对碳报告签名 | ✅ | ENTERPRISE |
| POST | `/signature/verify` | 验证碳报告签名 | ✅ | REVIEWER/THIRD_PARTY/ADMIN |
| POST | `/signature/encrypt` | 为审核员加密数据 | ✅ | ENTERPRISE |
| POST | `/signature/decrypt` | 解密数据 | ✅ | REVIEWER/ENTERPRISE |

---

### 09. 区块链管理 `/blockchain`

| 方法 | 路径 | 说明 | 认证 | 权限 |
|------|------|------|:----:|------|
| GET | `/blockchain/status` | 检查区块链连接状态 | ✅ | ADMIN/AUTHENTICATOR |
| GET | `/blockchain/block/{blockNumber}` | 查询区块信息 | ✅ | ADMIN/AUTHENTICATOR/THIRD_PARTY |
| GET | `/blockchain/transaction/{txHash}` | 查询链上交易 | ✅ | - |

---

### 10. 碳币交易管理 `/carbon-coin`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| GET | `/carbon-coin/account` | 获取碳币账户 | ✅ |
| POST | `/carbon-coin/recharge` | 充值碳币 | ✅ |
| POST | `/carbon-coin/transfer` | 碳币转账 | ✅ |
| GET | `/carbon-coin/transactions` | 交易流水 | ✅ |

---

### 11. 碳中和项目管理 `/carbon-neutral`

| 方法 | 路径 | 说明 | 认证 | 权限 |
|------|------|------|:----:|------|
| POST | `/carbon-neutral` | 创建项目 | ✅ | - |
| PUT | `/carbon-neutral/{id}` | 更新项目 | ✅ | - |
| GET | `/carbon-neutral/{id}` | 获取项目详情 | ✅ | - |
| GET | `/carbon-neutral/search` | 搜索项目 | ✅ | - |
| GET | `/carbon-neutral/my` | 我的项目 | ✅ | - |
| POST | `/carbon-neutral/{id}/submit` | 提交审核 | ✅ | - |
| POST | `/carbon-neutral/{id}/review` | 审核项目 | ✅ | ADMIN/REVIEWER |
| POST | `/carbon-neutral/{id}/start` | 启动实施 | ✅ | - |
| POST | `/carbon-neutral/{id}/submit-verification` | 申请核证 | ✅ | - |
| POST | `/carbon-neutral/verify` | 核证项目 | ✅ | VERIFIER/ADMIN |
| POST | `/carbon-neutral/{id}/use-credits` | 使用碳信用 | ✅ | - |
| PUT | `/carbon-neutral/{id}/monitoring` | 更新监测数据 | ✅ | - |
| POST | `/carbon-neutral/{id}/apply-certification` | 申请认证 | ✅ | - |
| POST | `/carbon-neutral/{id}/certify` | 完成认证 | ✅ | CERTIFIER/ADMIN |
| POST | `/carbon-neutral/{id}/terminate` | 终止项目 | ✅ | - |
| GET | `/carbon-neutral/pending-verification` | 待核证项目 | ✅ | VERIFIER/ADMIN |

---

### 12. 双向拍卖管理 `/auction`

| 方法 | 路径 | 说明 | 认证 | 权限 |
|------|------|------|:----:|------|
| POST | `/auction/buy` | 提交买入挂单 | ✅ | ENTERPRISE |
| POST | `/auction/sell` | 提交卖出挂单 | ✅ | ENTERPRISE |
| POST | `/auction/match` | 执行撮合 | ✅ | ADMIN |
| GET | `/auction/orders` | 查询挂单列表 | ✅ | - |
| GET | `/auction/my-orders` | 查询我的挂单 | ✅ | - |
| GET | `/auction/results` | 查询撮合结果 | ✅ | - |

---

### 13. 碳排放评级管理 `/emission`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| GET | `/emission/ratings/{enterpriseId}` | 企业评级历史 | ✅ |
| POST | `/emission/ratings` | 生成评级 | ✅ |
| GET | `/emission/rankings/{year}` | 行业排名 | ✅ |
| POST | `/emission/predict` | AI碳排放预测 | ✅ |

---

### 14. 搜索查询管理 `/search`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| GET | `/search/reports` | 搜索碳报告 | ✅ |
| GET | `/search/trades` | 搜索交易记录 | ✅ |
| GET | `/search/market-overview` | 市场概览 | ✅ |

---

### 15. 第三方监管管理 `/third-party`

| 方法 | 路径 | 说明 | 认证 | 权限 |
|------|------|------|:----:|------|
| GET | `/third-party/org-info` | 获取机构信息 | ✅ | THIRD_PARTY |
| GET | `/third-party/carbon-reports` | 查询碳报告 | ✅ | THIRD_PARTY |
| GET | `/third-party/statistics` | 监管统计数据 | ✅ | THIRD_PARTY |
| PUT | `/third-party/contact` | 更新联系方式 | ✅ | THIRD_PARTY |

---

### 16. 管理后台 `/admin`

| 方法 | 路径 | 说明 | 认证 | 权限 |
|------|------|------|:----:|------|
| GET | `/admin/users` | 查询用户列表 | ✅ | ADMIN |
| PUT | `/admin/users/{userId}/status` | 更新用户状态 | ✅ | ADMIN |
| GET | `/admin/dashboard` | 获取仪表板数据 | ✅ | ADMIN |
| GET | `/admin/statistics` | 获取系统统计数据 | ✅ | ADMIN |

---

## 错误码说明

### HTTP状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未授权/Token无效 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 429 | 请求过于频繁 |
| 500 | 服务器内部错误 |

### 业务错误码

| 错误码 | 说明 |
|--------|------|
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

---

## 附录

### 快速开始

```bash
# 1. 启动应用
cd oaiss-chain-backend
mvn spring-boot:run

# 2. 访问Swagger UI
open http://localhost:8080/api/v1/swagger-ui.html

# 3. 获取验证码
curl http://localhost:8080/api/v1/auth/captcha

# 4. 登录获取Token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 5. 使用Token访问API
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <your_token>"
```

### 相关链接

- Swagger UI: `http://localhost:8080/api/v1/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/v1/v1/api-docs`
- Actuator: `http://localhost:8080/api/v1/actuator`
- Prometheus: `http://localhost:8080/api/v1/actuator/prometheus`

---

*文档由系统自动生成，如有疑问请联系OAISS Team*
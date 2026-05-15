# OAISS CHAIN - 角色页面API映射矩阵

## 概览

本文档记录了OAISS CHAIN系统前后端的完整映射关系，包括：
- 5个角色类型的21个页面
- 每个页面的API调用
- API与后端Controller的对应关系
- 功能验证清单

---

## 角色分布

| 角色 | 中文名称 | 页面数量 | 目录 |
|------|---------|---------|------|
| ENTERPRISE | 企业 | 12 | views/enterprise/ |
| ADMIN | 管理员 | 4 | views/admin/ |
| AUDITOR | 审核员 | 1 | views/auditor/ |
| AUTHENTICATOR | 认证员 | 1 | views/authenticator/ |
| THIRD_PARTY | 第三方 | 1 | views/third-party/ |

---

## 一、ENTERPRISE 角色（企业）

### 1.1 CertificateManage.vue - 证书管理
**路径**: `views/enterprise/CertificateManage.vue`

| API调用 | API模块 | 后端Controller | 方法 | 功能 |
|---------|--------|---------------|------|------|
| `getCertificateList()` | api/certificate | CertificateController | GET /api/v1/certificates | 获取证书列表 |
| `uploadCertificate()` | api/certificate | CertificateController | POST /api/v1/certificates | 上传证书 |
| `deleteCertificate()` | api/certificate | CertificateController | DELETE /api/v1/certificates/{id} | 删除证书 |

**验证清单**:
- [ ] 证书列表正确加载并分页
- [ ] 上传证书功能正常（文件类型、大小限制）
- [ ] 删除证书有确认提示
- [ ] 权限控制：仅企业用户可访问

---

### 1.2 ChainBrowser.vue - 区块链浏览
**路径**: `views/enterprise/ChainBrowser.vue`

| API调用 | API模块 | 后端Controller | 方法 | 功能 |
|---------|--------|---------------|------|------|
| `getChainInfo()` | api/chain | ChainController | GET /api/v1/chain/info | 获取链信息 |
| `getBlockList()` | api/chain | ChainController | GET /api/v1/chain/blocks | 获取区块列表 |
| `getBlockDetail()` | api/chain | ChainController | GET /api/v1/chain/blocks/{id} | 获取区块详情 |

**验证清单**:
- [ ] 区块链基本信息正确显示
- [ ] 区块列表分页加载
- [ ] 区块详情查看功能
- [ ] 实时数据刷新机制

---

### 1.3 CarbonAsset.vue - 碳资产管理
**路径**: `views/enterprise/CarbonAsset.vue`

| API调用 | API模块 | 后端Controller | 方法 | 功能 |
|---------|--------|---------------|------|------|
| `getCarbonAssets()` | api/carbon | CarbonController | GET /api/v1/carbon/assets | 获取碳资产列表 |
| `getAssetDetail()` | api/carbon | CarbonController | GET /api/v1/carbon/assets/{id} | 获取资产详情 |

**验证清单**:
- [ ] 碳资产列表正确展示
- [ ] 资产详情信息完整
- [ ] 资产状态实时更新
- [ ] 数据统计图表准确

---

### 1.4 CarbonTrade.vue - 碳交易
**路径**: `views/enterprise/CarbonTrade.vue`

| API调用 | API模块 | 后端Controller | 方法 | 功能 |
|---------|--------|---------------|------|------|
| `getTradeList()` | api/trade | TradeController | GET /api/v1/trades | 获取交易列表 |
| `createTrade()` | api/trade | TradeController | POST /api/v1/trades | 创建交易 |
| `getTradeDetail()` | api/trade | TradeController | GET /api/v1/trades/{id} | 获取交易详情 |

**验证清单**:
- [ ] 交易列表分页展示
- [ ] 创建交易表单验证
- [ ] 交易详情查看
- [ ] 交易状态流转正确

---

### 1.5 DataUpload.vue - 数据上传
**路径**: `views/enterprise/DataUpload.vue`

| API调用 | API模块 | 后端Controller | 方法 | 功能 |
|---------|--------|---------------|------|------|
| `uploadData()` | api/data | DataController | POST /api/v1/data/upload | 上传数据 |
| `getUploadHistory()` | api/data | DataController | GET /api/v1/data/history | 获取上传历史 |

**验证清单**:
- [ ] 文件上传功能正常
- [ ] 支持的文件格式验证
- [ ] 上传进度显示
- [ ] 上传历史记录查看

---

### 1.6 EnterpriseInfo.vue - 企业信息
**路径**: `views/enterprise/EnterpriseInfo.vue`

| API调用 | API模块 | 后端Controller | 方法 | 功能 |
|---------|--------|---------------|------|------|
| `getEnterpriseInfo()` | api/enterprise | EnterpriseController | GET /api/v1/enterprises/{id} | 获取企业信息 |
| `updateEnterpriseInfo()` | api/enterprise | EnterpriseController | PUT /api/v1/enterprises/{id} | 更新企业信息 |

**验证清单**:
- [ ] 企业信息正确展示
- [ ] 信息编辑保存功能
- [ ] 必填字段验证
- [ ] 数据一致性校验

---

### 1.7 IssueRequest.vue - 发行申请
**路径**: `views/enterprise/IssueRequest.vue`

| API调用 | API模块 | 后端Controller | 方法 | 功能 |
|---------|--------|---------------|------|------|
| `createIssueRequest()` | api/issue | IssueController | POST /api/v1/issues | 创建发行申请 |
| `getIssueRequests()` | api/issue | IssueController | GET /api/v1/issues | 获取申请列表 |

**验证清单**:
- [ ] 发行申请表单完整
- [ ] 申请提交成功
- [ ] 申请列表展示
- [ ] 申请状态跟踪

---

### 1.8 OrganizationManage.vue - 组织管理
**路径**: `views/enterprise/OrganizationManage.vue`

| API调用 | API模块 | 后端Controller | 方法 | 功能 |
|---------|--------|---------------|------|------|
| `getOrganizations()` | api/organization | OrganizationController | GET /api/v1/organizations | 获取组织列表 |
| `createOrganization()` | api/organization | OrganizationController | POST /api/v1/organizations | 创建组织 |
| `updateOrganization()` | api/organization | OrganizationController | PUT /api/v1/organizations/{id} | 更新组织 |
| `deleteOrganization()` | api/organization | OrganizationController | DELETE /api/v1/organizations/{id} | 删除组织 |

**验证清单**:
- [ ] 组织树形结构展示
- [ ] 组织增删改功能
- [ ] 组织成员管理
- [ ] 权限分配正确

---

### 1.9 ReportGenerate.vue - 报告生成
**路径**: `views/enterprise/ReportGenerate.vue`

| API调用 | API模块 | 后端Controller | 方法 | 功能 |
|---------|--------|---------------|------|------|
| `generateReport()` | api/report | ReportController | POST /api/v1/reports/generate | 生成报告 |
| `getReportList()` | api/report | ReportController | GET /api/v1/reports | 获取报告列表 |
| `downloadReport()` | api/report | ReportController | GET /api/v1/reports/{id}/download | 下载报告 |

**验证清单**:
- [ ] 报告生成配置选项
- [ ] 报告生成进度显示
- [ ] 报告列表查看
- [ ] 报告下载功能

---

### 1.10 TransferManage.vue - 转移管理
**路径**: `views/enterprise/TransferManage.vue`

| API调用 | API模块 | 后端Controller | 方法 | 功能 |
|---------|--------|---------------|------|------|
| `getTransfers()` | api/transfer | TransferController | GET /api/v1/transfers | 获取转移列表 |
| `createTransfer()` | api/transfer | TransferController | POST /api/v1/transfers | 创建转移 |
| `getTransferDetail()` | api/transfer | TransferController | GET /api/v1/transfers/{id} | 获取转移详情 |

**验证清单**:
- [ ] 转移列表展示
- [ ] 创建转移流程
- [ ] 转移详情查看
- [ ] 转移状态更新

---

### 1.11 UserManage.vue - 用户管理
**路径**: `views/enterprise/UserManage.vue`

| API调用 | API模块 | 后端Controller | 方法 | 功能 |
|---------|--------|---------------|------|------|
| `getUsers()` | api/user | UserController | GET /api/v1/users | 获取用户列表 |
| `createUser()` | api/user | UserController | POST /api/v1/users | 创建用户 |
| `updateUser()` | api/user | UserController | PUT /api/v1/users/{id} | 更新用户 |
| `deleteUser()` | api/user | UserController | DELETE /api/v1/users/{id} | 删除用户 |
| `resetPassword()` | api/user | UserController | POST /api/v1/users/{id}/reset-password | 重置密码 |

**验证清单**:
- [ ] 用户列表分页展示
- [ ] 用户增删改功能
- [ ] 密码重置功能
- [ ] 角色分配正确

---

### 1.12 VerifyRequest.vue - 核验申请
**路径**: `views/enterprise/VerifyRequest.vue`

| API调用 | API模块 | 后端Controller | 方法 | 功能 |
|---------|--------|---------------|------|------|
| `createVerifyRequest()` | api/verify | VerifyController | POST /api/v1/verify/requests | 创建核验申请 |
| `getVerifyRequests()` | api/verify | VerifyController | GET /api/v1/verify/requests | 获取核验申请列表 |

**验证清单**:
- [ ] 核验申请表单
- [ ] 申请提交功能
- [ ] 申请列表查看
- [ ] 申请状态跟踪

---

## 二、ADMIN 角色（管理员）

### 2.1 DataStatistics.vue - 数据统计
**路径**: `views/admin/DataStatistics.vue`

| API调用 | API模块 | 后端Controller | 方法 | 功能 |
|---------|--------|---------------|------|------|
| `getStatistics()` | api/admin | AdminController | GET /api/v1/admin/statistics | 获取统计数据 |

**验证清单**:
- [ ] 统计数据正确展示
- [ ] 图表渲染准确
- [ ] 数据实时更新
- [ ] 导出功能正常

---

### 2.2 SystemCarbon.vue - 系统碳管理
**路径**: `views/admin/SystemCarbon.vue`

| API调用 | API模块 | 后端Controller | 方法 | 功能 |
|---------|--------|---------------|------|------|
| `getReportList()` | api/carbon | CarbonController | GET /api/v1/carbon/reports | 获取报告列表 |

**验证清单**:
- [ ] 报告列表正确展示
- [ ] 报告筛选功能
- [ ] 报告详情查看
- [ ] 管理员操作权限

---

### 2.3 SystemConfig.vue - 系统配置
**路径**: `views/admin/SystemConfig.vue`

| API调用 | API模块 | 后端Controller | 方法 | 功能 |
|---------|--------|---------------|------|------|
| 无API调用 | - | - | - | 使用localStorage本地存储 |

**验证清单**:
- [ ] 配置项保存到localStorage
- [ ] 配置项读取正确
- [ ] 配置项验证
- [ ] 页面刷新后配置保持

---

### 2.4 SystemUsers.vue - 系统用户管理
**路径**: `views/admin/SystemUsers.vue`

| API调用 | API模块 | 后端Controller | 方法 | 功能 |
|---------|--------|---------------|------|------|
| `getUserList()` | api/admin | AdminController | GET /api/v1/admin/users | 获取用户列表 |
| `updateUserStatus()` | api/admin | AdminController | PUT /api/v1/admin/users/{id}/status | 更新用户状态 |

**验证清单**:
- [ ] 用户列表正确展示
- [ ] 用户状态切换功能
- [ ] 用户搜索筛选
- [ ] 管理员权限验证

---

## 三、AUDITOR 角色（审核员）

### 3.1 AuditList.vue - 审核列表
**路径**: `views/auditor/AuditList.vue`

| API调用 | API模块 | 后端Controller | 方法 | 功能 |
|---------|--------|---------------|------|------|
| `getReportList()` | api/carbon | CarbonController | GET /api/v1/carbon/reports | 获取待审核报告列表 |
| `reviewReport()` | api/carbon | CarbonController | POST /api/v1/carbon/reports/{id}/review | 审核报告 |

**验证清单**:
- [ ] 待审核报告列表展示
- [ ] 报告详情查看
- [ ] 审核通过/拒绝操作
- [ ] 审核意见填写
- [ ] 审核记录保存

---

## 四、AUTHENTICATOR 角色（认证员）

### 4.1 VerifyList.vue - 核验列表
**路径**: `views/authenticator/VerifyList.vue`

| API调用 | API模块 | 后端Controller | 方法 | 功能 |
|---------|--------|---------------|------|------|
| 统计数据API | 待确认 | 待确认 | GET /api/v1/verify/statistics | 获取核验统计数据 |
| `getReportList()` | api/carbon | CarbonController | GET /api/v1/carbon/reports | 获取报告列表 |
| 审核操作API | 待确认 | 待确认 | POST /api/v1/verify/audit | 执行审核操作 |

**验证清单**:
- [ ] 核验统计数据正确显示
- [ ] 待核验报告列表展示
- [ ] 报告详情查看
- [ ] 核验操作功能
- [ ] 核验结果记录

**待确认**: 需要查看api目录下的完整API定义文件以确认具体接口

---

## 五、THIRD_PARTY 角色（第三方）

### 5.1 Monitor.vue - 监控页面
**路径**: `views/third-party/Monitor.vue`

| API调用 | API模块 | 后端Controller | 方法 | 功能 |
|---------|--------|---------------|------|------|
| `getCarbonReports()` | api/thirdParty | ThirdPartyController | GET /api/v1/third-party/carbon-reports | 获取碳报告数据 |
| `getStatistics()` | api/thirdParty | ThirdPartyController | GET /api/v1/third-party/statistics | 获取统计数据 |

**验证清单**:
- [ ] 碳报告数据正确展示
- [ ] 统计数据准确
- [ ] 数据图表渲染
- [ ] 第三方访问权限控制
- [ ] 数据隐私保护

---

## 后端Controller映射总览

| Controller | 路径前缀 | 主要功能 | 对应前端角色 |
|-----------|---------|---------|-------------|
| CertificateController | /api/v1/certificates | 证书管理 | ENTERPRISE |
| ChainController | /api/v1/chain | 区块链浏览 | ENTERPRISE |
| CarbonController | /api/v1/carbon | 碳资产/碳交易/碳报告 | ENTERPRISE, AUDITOR, ADMIN |
| TradeController | /api/v1/trades | 碳交易 | ENTERPRISE |
| DataController | /api/v1/data | 数据上传 | ENTERPRISE |
| EnterpriseController | /api/v1/enterprises | 企业信息 | ENTERPRISE |
| IssueController | /api/v1/issues | 发行申请 | ENTERPRISE |
| OrganizationController | /api/v1/organizations | 组织管理 | ENTERPRISE |
| ReportController | /api/v1/reports | 报告生成 | ENTERPRISE |
| TransferController | /api/v1/transfers | 转移管理 | ENTERPRISE |
| UserController | /api/v1/users | 用户管理 | ENTERPRISE |
| VerifyController | /api/v1/verify | 核验申请/核验 | ENTERPRISE, AUTHENTICATOR |
| AdminController | /api/v1/admin | 管理员功能 | ADMIN |
| ThirdPartyController | /api/v1/third-party | 第三方访问 | THIRD_PARTY |

---

## 验证执行计划

### Phase 1: ENTERPRISE角色验证 (12个页面)
优先级: 高 | 预计时间: 2-3天

1. 按页面顺序逐一验证功能
2. 记录发现的每个问题
3. 生成问题清单并分类

### Phase 2: ADMIN角色验证 (4个页面)
优先级: 中 | 预计时间: 1天

### Phase 3: AUDITOR角色验证 (1个页面)
优先级: 中 | 预计时间: 0.5天

### Phase 4: AUTHENTICATOR角色验证 (1个页面)
优先级: 中 | 预计时间: 0.5天

### Phase 5: THIRD_PARTY角色验证 (1个页面)
优先级: 中 | 预计时间: 0.5天

---

## 问题跟踪模板

### 问题记录格式
```markdown
### 问题 #[编号]: [简短描述]
- **页面**: [页面名称]
- **角色**: [角色类型]
- **严重程度**: [高/中/低]
- **问题描述**: [详细描述]
- **预期行为**: [应该如何]
- **实际行为**: [当前如何]
- **复现步骤**: 
  1. [步骤1]
  2. [步骤2]
- **相关API**: [API名称]
- **建议修复方案**: [修复建议]
- **状态**: [待修复/修复中/已修复]
```

---

## 附录

### A. API模块文件位置
- `oaiss-chain-frontend/src/api/admin.ts`
- `oaiss-chain-frontend/src/api/carbon.ts`
- `oaiss-chain-frontend/src/api/thirdParty.ts`
- `oaiss-chain-frontend/src/api/certificate.ts`
- `oaiss-chain-frontend/src/api/chain.ts`
- `oaiss-chain-frontend/src/api/trade.ts`
- `oaiss-chain-frontend/src/api/data.ts`
- `oaiss-chain-frontend/src/api/enterprise.ts`
- `oaiss-chain-frontend/src/api/issue.ts`
- `oaiss-chain-frontend/src/api/organization.ts`
- `oaiss-chain-frontend/src/api/report.ts`
- `oaiss-chain-frontend/src/api/transfer.ts`
- `oaiss-chain-frontend/src/api/user.ts`
- `oaiss-chain-frontend/src/api/verify.ts`

### B. 后端Controller文件位置
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/`

### C. 测试账号准备
需要准备不同角色的测试账号进行功能验证：
- [ ] ENTERPRISE角色账号
- [ ] ADMIN角色账号
- [ ] AUDITOR角色账号
- [ ] AUTHENTICATOR角色账号
- [ ] THIRD_PARTY角色账号

---

**文档版本**: v1.0  
**创建日期**: 2025-05-10  
**最后更新**: 2025-05-10  
**维护者**: AI Assistant

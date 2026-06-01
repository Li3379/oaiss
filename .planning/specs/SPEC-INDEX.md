---
status: draft
created: 2026-05-10
author: claude
source: Phase 2 UAT Verification
---

# SPEC索引：Phase 2 UAT发现的问题修复规范

## 背景

在Phase 2（Carbon Report Lifecycle）的UAT验证过程中，发现了若干需要优化的问题。本文档汇总了所有SPEC规范，用于指导后续修复工作。

## SPEC列表

| 编号 | 标题 | 优先级 | 状态 | 文件 |
|------|------|--------|------|------|
| SPEC-001 | 数据库配置统一化 | 🔴 高 | draft | [SPEC-001-database-config-unification.md](./SPEC-001-database-config-unification.md) |
| SPEC-002 | 测试数据清理机制 | 🔴 高 | draft | [SPEC-002-test-data-cleanup.md](./SPEC-002-test-data-cleanup.md) |
| SPEC-003 | 错误信息国际化与友好化 | 🟡 中 | draft | [SPEC-003-error-message-enhancement.md](./SPEC-003-error-message-enhancement.md) |
| SPEC-004 | 健康检查端点优化 | 🟡 中 | draft | [SPEC-004-health-endpoint.md](./SPEC-004-health-endpoint.md) |
| SPEC-005 | Token存储策略优化 | 🟢 低 | draft | [SPEC-005-token-storage.md](./SPEC-005-token-storage.md) |
| SPEC-006 | 验证码错误与过期错误码区分 | 🔴 高 | complete | [SPEC-006-captcha-error-differentiation.md](./SPEC-006-captcha-error-differentiation.md) |
| SPEC-007 | MinIO端口配置不一致 | 🔴 高 | complete | [SPEC-007-minio-port-configuration.md](./SPEC-007-minio-port-configuration.md) |
| SPEC-008 | 审核员角色权限不匹配 | 🔴 高 | complete | [SPEC-008-reviewer-role-permission-mismatch.md](./SPEC-008-reviewer-role-permission-mismatch.md) |
| SPEC-009 | EnterpriseController管理员无法访问 | 🟡 中 | complete | [SPEC-009-enterprise-controller-admin-access.md](./SPEC-009-enterprise-controller-admin-access.md) |
| SPEC-010 | Refresh Token错误码不规范 | 🟢 低 | complete | [SPEC-010-refresh-token-error-handling.md](./SPEC-010-refresh-token-error-handling.md) |

## 问题来源

所有问题均来自Phase 2 UAT验证过程：

### SPEC-001: 数据库配置不一致
- **发现场景**: Playwright验证时，Docker MySQL（3307）与本地MySQL（3306）数据分离
- **影响**: 开发环境数据混乱，测试脚本连接错误数据库
- **验证命令**: `docker port oaiss-mysql` 显示3307，但应用配置3306

### SPEC-002: 测试数据清理机制缺失
- **发现场景**: 多次运行测试，Emission Rating唯一约束冲突
- **影响**: 后续测试失败，需要手动清理数据库
- **验证命令**: `SELECT * FROM emission_rating WHERE enterprise_id=1 AND rating_year='2025'`

### SPEC-003: 错误信息不友好
- **发现场景**: Review提交失败，仅显示"该企业2025年评级已存在"
- **影响**: 用户无法定位具体问题
- **验证响应**: `{"code":3001,"message":"该企业2025年评级已存在"}`

### SPEC-004: 健康检查端点需要认证
- **发现场景**: 测试脚本使用Swagger UI做健康检查失败
- **影响**: 测试脚本维护困难
- **修复**: 已临时改为使用login端点

### SPEC-005: Token存储使用sessionStorage
- **发现场景**: Playwright验证时发现Token存储机制
- **影响**: 刷新页面需要重新登录
- **验证代码**: `sessionStorage.setItem('access_token', access)`

## 实施顺序

建议按优先级顺序实施：

### 第一阶段（高优先级）

1. **SPEC-001**: 数据库配置统一化
   - 影响范围：全局
   - 实施时间：约2小时
   - 风险：数据迁移需谨慎

2. **SPEC-002**: 测试数据清理机制
   - 影响范围：测试流程
   - 实施时间：约1小时
   - 风险：低

### 第二阶段（中优先级）

3. **SPEC-003**: 错误信息优化
   - 影响范围：用户体验
   - 实施时间：约3小时
   - 风险：低

4. **SPEC-004**: 健康检查端点
   - 影响范围：运维、测试
   - 实施时间：约1小时
   - 风险：低

### 第三阶段（低优先级）

5. **SPEC-005**: Token存储优化
   - 影响范围：用户体验
   - 实施时间：约2小时
   - 风险：中（安全相关）

## 审核流程

每个SPEC需经过以下流程：

1. **Draft** → 规范文档编写完成
2. **Review** → 团队审核确认
3. **Approved** → 批准实施
4. **Implementing** → 正在实施
5. **Complete** → 实施完成并验证

## 验证标准

每个SPEC完成后需满足：

- [ ] 代码变更符合规范文档
- [ ] 单元测试覆盖
- [ ] 集成测试通过
- [ ] 无回归问题
- [ ] 文档更新

## 下一步

1. 用户审核SPEC文档
2. 确认实施方案
3. 按优先级顺序实施
4. 每个SPEC完成后更新状态

---

请审核以上SPEC规范文档，确认是否需要调整或补充，然后我将开始按优先级实施代码变更。
---
status: ready
phase: fix-phase2-issues
source: Phase 2 UAT Verification
created: 2026-05-10
---

# Phase: Fix Phase 2 UAT Issues

## Goal

修复Phase 2 UAT验证过程中发现的5个问题，提升系统稳定性和用户体验。

## Scope

基于SPEC文档实施以下修复：

| 优先级 | SPEC | 问题 | 预计时间 |
|--------|------|------|----------|
| 🔴 高 | SPEC-001 | 数据库配置统一化 | ~2小时 |
| 🔴 高 | SPEC-002 | 测试数据清理机制 | ~1小时 |
| 🟡 中 | SPEC-003 | 错误信息优化 | ~3小时 |
| 🟡 中 | SPEC-004 | 健康检查端点 | ~1小时 |
| 🟢 低 | SPEC-005 | Token存储优化 | ~2小时 |

## Canonical References

**必须阅读以下SPEC文档：**
- `.planning/specs/SPEC-001-database-config-unification.md` — 数据库配置方案
- `.planning/specs/SPEC-002-test-data-cleanup.md` — 测试数据清理方案
- `.planning/specs/SPEC-003-error-message-enhancement.md` — 错误信息优化方案
- `.planning/specs/SPEC-004-health-endpoint.md` — 健康检查端点方案
- `.planning/specs/SPEC-005-token-storage.md` — Token存储方案

## Implementation Decisions

### SPEC-001: 数据库配置
- 采用方案A+C组合：Docker作为主环境 + Profile支持本地开发
- 修改docker-compose.yml端口映射为3306:3306
- 添加application-local.yml支持本地开发
- 提供数据迁移脚本

### SPEC-002: 测试数据清理
- 创建scripts/cleanup-test-data.sh脚本
- 定义测试数据命名规范（TEST-、UAT-前缀）
- 集成到现有测试脚本

### SPEC-003: 错误信息优化
- 扩展ErrorCode枚举使用messageKey
- 创建国际化资源文件（中英文）
- 创建MessageUtils工具类
- 修改BusinessException支持动态参数

### SPEC-004: 健康检查端点
- 创建HealthController
- 添加/health、/health/ready、/health/live端点
- 配置安全白名单
- 更新测试脚本

### SPEC-005: Token存储
- 使用localStorage + 过期时间
- 添加"记住我"选项
- 创建Token管理工具函数
- 添加Token刷新机制

## Dependencies

- 无外部依赖
- 所有修复向后兼容

## Success Criteria

1. Docker MySQL运行在3306端口
2. 测试脚本可重复运行无冲突
3. 错误信息包含上下文（企业名、年份等）
4. /health端点无需认证可访问
5. Token刷新页面保持登录状态

## Risks

| 风险 | 缓解措施 |
|------|----------|
| 数据迁移丢失 | 先备份，验证后再删除本地MySQL |
| 测试数据误删 | 使用明确命名规范，只删除匹配模式 |
| Token安全 | 设置合理过期时间，支持主动登出 |

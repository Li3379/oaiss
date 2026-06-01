# Staging Deploy Evidence

> Status: skeleton only
> Copy this file and replace `YYYY-MM-DD` with the real execution date before use.

参考模板：

- [external-execution-evidence-template.md](C:/Users/LiShuai/Desktop/OAISS%20CHAIN/docs/external-execution-evidence-template.md)

建议本文件用于覆盖以下 acceptance 开放项：

- GHCR release images have been published with current successful evidence.
- GitHub `staging` environment secrets have been populated with real values.
- Remote staging host bootstrap has been executed successfully.
- Fabric secrets have been placed on the remote staging host.
- First staging deployment has been executed successfully.
- Staging health checks have been verified against the real remote URLs.

---

## 1. Basic Info

- 执行日期：
- 执行人：
- 复核人：
- 目标环境：`staging`
- 目标主机：
- Commit SHA：

## 2. Release Images

- Actions Run URL：
- backend image：
- frontend image：
- ml-service image：
- 结果摘要：

## 3. Environment Secrets

- `staging` Environment 已配置：是 / 否
- 复核结论：

## 4. Remote Host Bootstrap

- bootstrap 命令：
- 目录检查结论：
- Fabric 文件检查结论：

## 5. Deployment

- deploy-release Run URL：
- `environment=staging`：是 / 否
- 部署结果：
- 自动回滚是否触发：

## 6. Health Checks

- 前端：
- 后端：
- ML：
- Fabric 状态：

## 7. Attachments

- 截图：
- 日志：
- 输出摘录：

## 8. Sign-off

- Engineering：
- QA：
- Ops：
- 结论：

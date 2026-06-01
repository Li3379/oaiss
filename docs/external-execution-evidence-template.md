# 外部执行证据回填模板

本文档用于在 OAISS CHAIN 进入远程 `staging` / `production` 发布阶段后，
把原本依赖真实环境的执行项，回填成可复核、可签收、可追溯的闭环证据。

使用原则：

- 每完成一次真实外部执行，就复制本模板形成一份带日期的记录文件
- 建议命名：
  - `docs/evidence/staging-deploy-YYYY-MM-DD.md`
  - `docs/evidence/production-deploy-YYYY-MM-DD.md`
  - `docs/evidence/production-observation-YYYY-MM-DD.md`
- 不要只写“已完成”，必须写明执行人、时间、命令/工作流、URL、结果和附件位置
- 如涉及截图、日志、GitHub Actions Run、告警截图、回滚记录，请一并附上路径或链接
- 推荐先阅读 `docs/evidence/README.md`
- 第 11 节中的 acceptance 条目文本请保持原样，便于 `node scripts/closure-audit.mjs` 自动比对

---

## 1. 基本信息

- 执行日期：
- 执行时区：
- 执行人：
- 审核人：
- 环境：`staging` / `production`
- 目标主机：
- Git 分支 / Commit SHA：

## 2. 镜像发布证据

- GitHub Actions 工作流：`.github/workflows/release-images.yml`
- Run ID / Run URL：
- `image_tag` 输入值：
- `push_latest` 输入值：
- backend image：
- frontend image：
- ml-service image：
- 结果摘要：

附件：

- Actions 截图：
- Step Summary 截图：

## 3. GitHub Environment 密钥完成情况

- Environment 名称：
- `DEPLOY_HOST` 已配置：是 / 否
- `DEPLOY_PORT` 已配置：是 / 否
- `DEPLOY_USER` 已配置：是 / 否
- `DEPLOY_SSH_KEY` 已配置：是 / 否
- `DEPLOY_TARGET_DIR` 已配置：是 / 否
- `DEPLOY_ENV_FILE` 已配置：是 / 否
- `FRONTEND_HEALTHCHECK_URL` 已配置：是 / 否
- `BACKEND_HEALTHCHECK_URL` 已配置：是 / 否
- `REGISTRY_HOST/USERNAME/PASSWORD` 如需已配置：是 / 否 / 不适用
- 填写人：
- 复核人：
- 结果摘要：

注意：

- 不要把真实 secret 明文写入本文档
- 这里只记录“已配置且已由操作人确认不是占位符”

## 4. 远程主机准备证据

- bootstrap 命令：
- 执行时间：
- 执行结果：
- 远程目录：
- `backups/` 已创建：是 / 否
- `scripts/` 已创建：是 / 否
- `runtime-logs/backend` 已创建：是 / 否
- `runtime-logs/frontend` 已创建：是 / 否
- `runtime-logs/ml-service` 已创建：是 / 否
- `secrets/fabric` 已创建：是 / 否
- Fabric 文件已放置：是 / 否
- 结果摘要：

附件：

- 终端输出截图：
- 远程 `ls -R` 或等价证据：

## 5. 部署执行证据

- GitHub Actions 工作流：`.github/workflows/deploy-release.yml`
- Run ID / Run URL：
- `environment` 输入值：
- `backend_image` override：
- `frontend_image` override：
- `ml_service_image` override：
- 部署开始时间：
- 部署结束时间：
- 结果：成功 / 失败 / 部分成功
- 自动回滚是否触发：是 / 否
- 结果摘要：

附件：

- Actions 截图：
- Step Summary 截图：
- 失败时远程 `ps/logs` 摘要：

## 6. 健康检查证据

- 前端健康检查 URL：
- 前端健康检查时间：
- 前端健康检查结果：
- 后端健康检查 URL：
- 后端健康检查时间：
- 后端健康检查结果：
- ML 健康检查方式：
- ML 健康检查结果：
- Fabric 状态接口检查：
- 结果摘要：

附件：

- `curl` 输出：
- 截图或日志：

## 7. 业务验收证据

- 非演示账号登录验证：通过 / 失败
- 企业侧核心业务链路：通过 / 失败
- ML 依赖链路：通过 / 失败
- 区块链相关链路：通过 / 失败 / 不适用
- 抽查人：
- 抽查时间：
- 结果摘要：

建议记录至少一条具体链路：

1. 使用的账号角色：
2. 打开的页面 / 调用的接口：
3. 实际输入：
4. 预期结果：
5. 实际结果：

附件：

- 页面截图：
- API 响应摘录：

## 8. 观察窗口证据

- 观察窗口开始时间：
- 观察窗口结束时间：
- 总时长：
- 登录成功率：
- 报表提交成功率：
- 后端 5xx 比例：
- ML 推理错误率：
- 关键 API `p95`：
- 是否触发回滚阈值：是 / 否
- 结果摘要：

附件：

- 监控截图：
- 告警记录：
- 观察窗口值班记录：

## 9. 回滚演练证据

- 回滚触发原因：演练 / 故障 / 其他
- 回滚使用镜像：
- 回滚执行方式：
- 回滚开始时间：
- 回滚结束时间：
- 回滚后前端健康检查：通过 / 失败
- 回滚后后端健康检查：通过 / 失败
- 回滚后核心业务链路：通过 / 失败
- 结果摘要：

附件：

- Actions 截图：
- 健康检查输出：
- 回滚后业务截图：

## 10. 最终签收

- Engineering 签收：通过 / 不通过
- QA 签收：通过 / 不通过
- Ops 签收：通过 / 不通过
- Product / Owner 签收：通过 / 不通过
- 是否允许进入下一阶段：是 / 否
- 最终结论：

## 11. 对照 acceptance 项

请明确本次证据覆盖了以下哪些开放项：

- [ ] GHCR release images have been published with current successful evidence.
- [ ] GitHub `staging` environment secrets have been populated with real values.
- [ ] GitHub `production` environment secrets have been populated with real values.
- [ ] Remote staging host bootstrap has been executed successfully.
- [ ] Fabric secrets have been placed on the remote staging host.
- [ ] First staging deployment has been executed successfully.
- [ ] Staging health checks have been verified against the real remote URLs.
- [ ] Staging business rehearsal has been completed successfully.
- [ ] Staging rollback rehearsal has been completed successfully.
- [ ] Production deployment has been executed successfully.
- [ ] Production health checks have been verified against the real public URLs.
- [ ] Production observation window has completed with acceptable metrics.
- [ ] Production rollback path has been validated with real evidence or signed operational proof.
- [ ] Every acceptance item above is checked with current authoritative evidence.
- [ ] OAISS CHAIN can be claimed as 100% closed-loop complete without relying on missing external execution evidence.

# OAISS CHAIN 最终验收清单

本清单是当前 OAISS CHAIN 生产就绪工作面向操作侧的最终收口文档。

## 1. 当前状态

### 已完成的防护与约束

- 已将历史遗留的 `AUTHENTICATOR` 运行时角色从现行角色基线中移除
- 已将 `local,fabric` 与远程 `staging,fabric` / `prod,fabric` 路径分别文档化
- 已新增生产 compose 包装脚本：
  - `scripts/prod-compose.ps1`
  - `scripts/prod-compose.sh`
- 已新增生产环境模板校验脚本：
  - `scripts/validate-prod-env.mjs`
- 已新增仓库侧统一闭环审计脚本：
  - `scripts/closure-audit.mjs`
- 已将生产 compose 干跑校验加入 CI
- 已新增纯镜像远程 compose 部署文件：
  - `docker-compose.release.yml`
- 已将后端生产配置契约测试加入 CI
- 已将 ML 共享密钥校验加入 CI
- 已新增 release 镜像工作流：
  - `.github/workflows/release-images.yml`
- 已新增远程发布工作流：
  - `.github/workflows/deploy-release.yml`
- 已将严格发布前校验、release host bootstrap smoke、closure audit 接入 CI
- 已补齐 staging / production 运维运行手册

## 2. `local,fabric` 验收基线

基于当前工作站实测（2026-05-30，Asia/Shanghai）：

- Docker Desktop 运行正常
- MySQL 正常
- Redis 正常
- MinIO 正常
- ML 服务正常
- Fabric orderer / peer / CA / CouchDB 正常
- 后端监听 `:8080`
- 前端开发服务器监听 `:5173`

### 已验证探针

1. 后端 actuator：
   - `GET /api/v1/actuator/health`
   - 结果：`UP`
2. ML 健康检查：
   - `GET /health`
   - 结果：`healthy`
3. 本地全量健康脚本：
   - `bash ./scripts/health-check.sh`
   - 结果：在修复 WSL 前端探针回退逻辑后通过
4. 登录 smoke：
   - `bash ./scripts/login-test.sh`
   - 结果：6/6 账号通过
5. 区块链浏览探针：
   - `bash ./scripts/blockchain-test.sh`
   - 结果：单独执行时通过

## 3. 重要测试执行规则

不要并行运行 `scripts/login-test.sh` 与 `scripts/blockchain-test.sh`，也不要让它们同时使用同一组种子账号。

原因如下：

- `login-test.sh` 会执行登出与 token 黑名单验证
- `blockchain-test.sh` 会复用种子登录 token
- 并行执行时，登出副作用可能使另一侧 token 失效，造成假阴性失败

推荐执行顺序：

1. `bash ./scripts/health-check.sh`
2. `bash ./scripts/login-test.sh`
3. `bash ./scripts/blockchain-test.sh`

## 4. 生产发布前置条件

在 staging 或 production 部署前，至少满足以下条件：

1. CI 全部通过
2. `node scripts/validate-prod-env.mjs` 校验通过，且真实发布 env 需额外通过 `--require-real-secrets`
3. `node scripts/closure-audit.mjs` 明确显示仓库侧结构完整，且剩余项仅为外部执行证据
4. 通过包装脚本执行的生产 compose 干跑通过
5. 填写完成的环境文件存放在仓库外
6. 备份已完成
7. 监控 / 告警链路已确认

## 5. Staging / Production 命令

请使用部署运行手册：

- `docs/deployment-runbook.md`
- `docs/remote-staging-first-deploy-checklist.md`

推荐包装脚本用法：

Windows：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\prod-compose.ps1 -EnvFile C:\secure\oaiss-chain-prod.env -ComposeArgs config
```

Linux / macOS：

```bash
./scripts/prod-compose.sh --env-file /secure/path/oaiss-chain-prod.env config
```

## 6. 仍需人工确认的事项

以下项目仍需要目标环境中的操作人员进行最终判断：

- 填写的密钥值都是真实值、已轮换，且不是占位符
- 公网 DNS / TLS 终止配置正确
- 托管 MySQL / Redis / 对象存储网络访问正确
- 远程发布所需 GitHub environment secrets 配置正确
- 业务 canary 阈值已明确，且值守责任人已安排
- 非演示生产账号已准备好用于 smoke 验证

## 7. 签收建议

建议按以下顺序完成发布签收：

1. Engineering：配置、健康检查、smoke 探针
2. QA：按角色执行登录与核心旅程抽查
3. Ops：备份、部署、指标观察窗口
4. Product / Owner：最终上线批准

当前汇总后的闸门状态请参考：

- `docs/go-live-gate-matrix.md`

外部执行证据建议统一回填到：

- `docs/external-execution-evidence-template.md`
- `docs/evidence/README.md`

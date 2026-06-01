# 上线闸门矩阵

本矩阵用于汇总 OAISS CHAIN 当前的生产就绪状态。

它将工作拆分为两类：

- 仓库侧已完成的就绪建设
- 环境侧尚需真实执行的上线工作

## 1. 闸门总览

| 闸门 | 范围 | 当前状态 | 证据 / 文档 |
|---|---|---|---|
| 生产 profile 加固 | repo | 已完成 | `application-prod.yml`、`SecurityStartupValidator`、`docs/production-readiness.md` |
| 生产环境模板 | repo | 已完成 | `.env.prod.example`、`.env.staging.example` |
| 纯镜像远程 compose 部署 | repo | 已完成 | `docker-compose.release.yml` |
| Compose 包装脚本 | repo | 已完成 | `scripts/prod-compose.sh`、`scripts/prod-compose.ps1` |
| 模板校验脚本 | repo | 已完成 | `scripts/validate-prod-env.mjs` |
| CI 生产配置检查 | repo | 已完成 | `.github/workflows/e2e-tests.yml` |
| 严格发布前 secrets 守门 | repo | 已完成 | `scripts/validate-prod-env.mjs`、`.github/workflows/deploy-release.yml`、`.github/workflows/e2e-tests.yml` |
| 远程发布主机初始化 smoke | repo | 已完成 | `scripts/bootstrap-remote-release-host.sh`、`.github/workflows/e2e-tests.yml` |
| 仓库侧闭环统一审计入口 | repo | 已完成 | `scripts/closure-audit.mjs`、`tracks/phase-01-acceptance.md` |
| Release 镜像发布工作流 | repo | 已完成 | `.github/workflows/release-images.yml` |
| 远程部署工作流 | repo | 已完成 | `.github/workflows/deploy-release.yml` |
| 部署密钥校验 | repo | 已完成 | `.github/workflows/deploy-release.yml` |
| 可选仓库登录路径 | repo | 已完成 | `.github/workflows/deploy-release.yml` |
| Staging 演练手册 | repo | 已完成 | `docs/remote-staging-rehearsal.md` |
| Staging 首次上线操作单 | repo | 已完成 | `docs/remote-staging-first-deploy-checklist.md` |
| Staging GitHub 密钥填写单 | repo | 已完成 | `docs/staging-github-secrets-fillout.md` |
| Production GitHub 密钥填写单 | repo | 已完成 | `docs/production-github-secrets-fillout.md` |
| 远程主机预检清单 | repo | 已完成 | `docs/remote-host-preflight-checklist.md` |
| 观察窗口 / 回滚阈值 | repo | 已完成 | `docs/production-observation-window.md` |
| `local,fabric` 验证基线 | local execution | 已完成 | `docs/final-acceptance-checklist.md` |
| GHCR 镜像实际发布 | external execution | 待执行 | 运行 `release-images.yml` |
| GitHub staging secrets 已配置 | external execution | 待执行 | 完成 GitHub Environment 设置 |
| GitHub production secrets 已配置 | external execution | 待执行 | 完成 GitHub Environment 设置 |
| 远程 staging 部署已执行 | external execution | 待执行 | 运行 `deploy-release.yml` 并设置 `environment=staging` |
| 远程 staging 业务验证已执行 | external execution | 待执行 | 按 `docs/remote-staging-rehearsal.md` 执行 |
| 远程 staging 回滚演练已执行 | external execution | 待执行 | 按 `docs/remote-staging-rehearsal.md` 执行 |
| 生产部署已执行 | external execution | 待执行 | 运行 `deploy-release.yml` 并设置 `environment=production` |
| 生产观察窗口已完成 | external execution | 待执行 | 按 `docs/production-observation-window.md` 执行 |

## 2. 状态定义

### 已完成

表示仓库中已经具备完成该闸门所需的代码、工作流、脚本或文档。

### 待执行

表示剩余工作依赖真实环境访问、真实密钥、真实远程主机或真实已发布镜像，因此无法仅靠仓库内改动完成。

## 3. 当前实际结论

当前状态更准确地说是：

- 仓库侧发布工程能力：已完整建成并可重复复核
- 环境侧真实发布与签收：尚未实际执行完毕

因此，OAISS CHAIN 目前应被视为：

- **已具备进入远程分阶段发布准备的条件**
- **尚未被证明为完整完成正式上线**

## 4. 下一步必做顺序

1. 创建 GitHub Environment `staging`
2. 按 `docs/staging-github-secrets-fillout.md` 填写密钥
3. 使用 `.github/workflows/release-images.yml` 发布 release 镜像
4. 使用 `.github/workflows/deploy-release.yml` 部署到远程 staging
5. 执行 staging 业务验证与回滚演练
6. 创建或更新 GitHub Environment `production`
7. 按 `docs/production-github-secrets-fillout.md` 填写密钥
8. 执行生产部署
9. 完成生产观察窗口

## 5. 主要参考文档

- `docs/production-readiness.md`
- `docs/deployment-runbook.md`
- `docs/remote-staging-rehearsal.md`
- `docs/remote-staging-first-deploy-checklist.md`
- `docs/production-observation-window.md`
- `docs/remote-host-preflight-checklist.md`
- `docs/external-execution-evidence-template.md`
- `docs/evidence/README.md`
- `docs/closure-verification-2026-05-31.md`
- `tracks/phase-01-acceptance.md`

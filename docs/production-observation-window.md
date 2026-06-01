# 生产发布观察窗口

本文档定义了 OAISS CHAIN 在生产环境完成部署后的首轮观察窗口实践。

它的目标是把目前“观察 canary”这种笼统要求，落成明确的操作检查项与回滚触发条件。

## 1. 观察窗口时长

建议最少满足以下要求：

- 低流量发布：30 分钟
- 正常业务发布：60 分钟
- 涉及报表、交易、认证、ML 或 Fabric 的改动：至少覆盖 1 个完整业务周期

在观察窗口结束前，不应宣布本次发布完成。

## 2. 需要重点观察的核心指标

### 2.1 可用性

重点关注：

- 前端 `/health`
- 后端 `/api/v1/actuator/health`
- ML `/health`

预期状态：

- 所有健康检查地址持续可用
- Docker 中没有反复重启的容器

### 2.2 错误率

重点关注：

- 后端 HTTP 5xx 比例
- 前端错误日志 / JS 致命错误
- ML 推理失败率

建议回滚触发条件：

- 后端 5xx 比例连续 5 分钟高于 2%
- ML 推理错误率连续 5 分钟高于 5%
- 用户刷新后仍反复出现前端可见的致命错误

### 2.3 延迟

重点关注：

- 登录 `p95`
- 核心业务 API `p95`
- ML 预测 API `p95`

建议回滚触发条件：

- 登录 `p95` 连续 10 分钟高于基线 3 倍
- 核心业务 API `p95` 连续 10 分钟高于基线 2 倍
- 若业务强依赖 ML，ML API `p95` 连续 10 分钟高于基线 2 倍

### 2.4 业务成功率

重点关注：

- 登录成功率
- 碳报表提交成功率
- 报表详情页打开成功率
- 交易创建 / 查询成功率
- ML 预测成功率
- 启用 `prod,fabric` 时的 Fabric 提交 / 查询成功率

建议回滚触发条件：

- 登录成功率低于 95%
- 报表提交成功率低于 95%
- 任一关键角色无法完整跑通一条主流程

## 3. 建议执行的人工抽查

至少使用真实、非演示账号完成以下抽查：

1. 企业账号登录
2. 企业打开一个主要工作页面
3. 企业提交或编辑一份真实报表
4. 审核员或管理员打开对应审核 / 管理页面
5. 至少一个依赖 ML 的页面返回符合预期的结果
6. 如启用 Fabric，至少一条区块链查询成功

## 4. 快速命令

基础健康检查：

```bash
curl -f https://app.example.com/health
curl -f https://app.example.com/api/v1/actuator/health
```

远程主机上的容器状态：

```bash
docker compose --env-file /opt/oaiss-chain-prod/oaiss-chain.env -f /opt/oaiss-chain-prod/docker-compose.release.yml ps
docker compose --env-file /opt/oaiss-chain-prod/oaiss-chain.env -f /opt/oaiss-chain-prod/docker-compose.release.yml logs --tail=200
```

## 5. 回滚决策规则

只要出现以下任一情况，就应立即回滚：

- 健康检查失败，且未能快速恢复
- 合法的非演示用户无法登录
- 关键核心业务流程反复失败
- 后端或 ML 错误率超过上述阈值并持续升高
- 启用 Fabric 的生产环境无法完成必要的提交 / 查询操作

面对生产环境用户可见故障，应倾向谨慎回滚，而不是盲目乐观等待。

## 6. 回滚执行方式

推荐回滚路径：

1. 选定上一个已知稳定的镜像版本
2. 重新运行 `.github/workflows/deploy-release.yml`
3. 通过以下输入传入旧镜像标签：
   - `backend_image`
   - `frontend_image`
   - `ml_service_image`
4. 再次验证健康检查地址
5. 重跑一条核心人工业务流程

## 7. 退出标准

只有满足以下条件，才可认为本次生产发布稳定：

- 观察窗口已完整结束
- 健康状态始终稳定
- 没有触发任何回滚条件
- 人工业务抽查通过
- 告警链路在此期间保持正常

建议在观察窗口结束后立即复制并填写：

- `docs/external-execution-evidence-template.md`

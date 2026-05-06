# Phase 2: Outer layer — multiple Prompt schemes

## Prompt Schemes

### Scheme A — 用户原始表述（Baseline / Dimension 1: L1 抽象层）

> "该项目没有经过实际的前后端联调和完整的整合过程，所以会有许多问题，我需要你为我出谋划策。"

要求：修复所有阻碍前后端联调的问题，使系统可以端到端运行。

### Scheme B — 技术聚焦（Dimension 1: L3 + Dimension 3: 示例驱动）

> 给定 OAISS Chain 项目（Spring Boot 3.2.5 + Vue 3 + Element Plus + MySQL + Redis + JWT），当前状态：
>
> 1. 前端 `VITE_API_BASE_URL` 未配置，后端 context-path 为 `/api/v1`
> 2. 前端响应拦截器检查 `response.data` 但后端返回 `{code, message, data}` 包装
> 3. 分页格式：前端期望 `{items, total}`，后端返回 `Page<T>` 包在 `ApiResponse.data` 中
> 4. SecurityConfig 路径规则 `/enterprise/**`、`/review/**` 与实际 Controller `/carbon/**`、`/trade/**` 不匹配
> 5. JWT payload 缺少 `roles`/`userId`/`enterpriseId` 字段导致前端 store 解析失败
>
> 修复以上问题，使登录→碳报告查询→P2P交易的完整流程可走通。

### Scheme C — 生产就绪导向（Dimension 2: Robustness + Dimension 4: 安全工程师角色）

> 你是一名专注于安全和可靠性的高级后端工程师。当前 OAISS Chain 项目存在以下风险：
>
> - JWT secret 硬编码 fallback、CORS 全开放、Actuator 公开暴露
> - Flyway 配置了但没有迁移文件，ddl-auto 使用 update 模式
> - 没有 docker-compose，无法一键启动完整环境
>
> 在修复联调问题的同时，确保安全配置达标：JWT secret 环境变量化、CORS 收窄、Actuator 保护、Flyway 迁移脚本就位、docker-compose 编排完整。

---

## Feature list (all schemes)

1. 用户认证（登录/注册/登出/JWT/Token刷新/验证码）
2. 碳核算（报告 CRUD/提交审核/审核）
3. 碳交易（P2P/双向拍卖/撮合/订单管理）
4. 碳币（账户/充值/转账/流水）
5. 信誉评分（评分/事件/扣分）
6. 数字签名（RSA 密钥管理/签名/验签）
7. 碳中和项目（项目 CRUD/认证）
8. 碳排放评级
9. 排放数据管理
10. 文件上传（MinIO）
11. 全文搜索
12. 管理后台（用户管理/碳核算管理/系统配置/统计）
13. 角色仪表盘（企业/审核员/认证员/第三方/管理员）
14. 数据可视化
15. 区块链浏览器
16. Swagger API 文档
17. 统一响应格式 / CORS / 全局异常处理

## Tech stack (all schemes)

- Backend: Spring Boot 3.2.5, Spring Data JPA, MySQL 8, Spring Security, JWT (jjwt 0.12.5), Redis, MinIO, Flyway, SpringDoc OpenAPI
- Frontend: Vue 3 (Composition API), Vite, Element Plus, Pinia, Axios
- Infrastructure: Docker Compose, MySQL, Redis, MinIO

## Output type (all schemes)

Full-stack web application — Vue SPA frontend + REST API backend

---

## Scheme consistency check

| 维度 | Scheme A | Scheme B | Scheme C |
|------|----------|----------|----------|
| 功能范围 | 全部 17 项 | 全部 17 项 | 全部 17 项 |
| 技术栈 | Spring Boot + Vue 3 | Spring Boot + Vue 3 | Spring Boot + Vue 3 |
| 输出类型 | 全栈 Web | 全栈 Web | 全栈 Web |
| 差异维度 | L1 抽象（一句话） | L3 技术+示例 | Robustness+角色 |

```
□ Same feature scope: A=[1-17], B=[1-17], C=[1-17] → match ✓
□ Same tech stack: all Spring Boot 3.2.5 + Vue 3 → match ✓
□ Same output type: all full-stack web → match ✓
□ Same acceptance checklist: all satisfy tracks/phase-01-acceptance.md → match ✓
□ Differences only in phrasing: A=L1, B=L3+examples, C=robustness+role → OK ✓

Result: pass
```

## Scheme–dimension table

| Scheme | Track dir | Phase-2 dimension | Note |
|--------|-----------|-------------------|------|
| A | tracks/prompt-a | L1 抽象（用户原始） | Baseline |
| B | tracks/prompt-b | L3 技术 + 示例驱动 | 列出具体技术问题 |
| C | tracks/prompt-c | Robustness + 安全工程师角色 | 附加安全/部署维度 |

# Phase 10: 准入与资格证 - Context

**Gathered:** 2026-05-15
**Status:** Ready for planning

<domain>
## Phase Boundary

管理员可签发企业准入证书（EnterpriseAdmission）和审核员资格证（ReviewerQualification），企业和审核员可在前端查看自身证书状态（已签发/已吊销）。本阶段新增 2 个 Service、AdminController 新增端点、1 个前端管理页面 + 2 个角色视图、1 个 Flyway 迁移。依赖 Phase 6（v1.0 complete），与 Phase 9 独立。

**Requirements:** REQ-07（签发准入证书）、REQ-08（签发审核员资格证）

**Success Criteria:**
1. 管理员可通过 Admin API 签发企业准入证书（EnterpriseAdmission），证书状态可在数据库查询
2. 管理员可通过 Admin API 签发审核员资格证（ReviewerQualification），资格证状态可在数据库查询
3. 企业用户可在前端查看自身准入证书状态（已签发/未签发/已吊销）
4. 审核员可在前端查看自身资格证状态（已签发/未签发/已吊销）

</domain>

<decisions>
## Implementation Decisions

### D-01: EnterpriseAdmission 是新实体，不复用 EntryPermission

现有 `EntryPermission` 实体是 API 访问控制表（userType, apiPath, httpMethod, allowed），与企业准入证书语义完全不同。创建新实体 `EnterpriseAdmission`，字段：enterpriseId, certificateNo, issuedDate, status。保留 `EntryPermission` 不动。

### D-02: 证书生命周期 — ACTIVE + REVOKED 两种状态

两个证书实体统一使用：
- `status = 1` — 有效（ACTIVE / 已签发）
- `status = 2` — 已吊销（REVOKED）
- 不存在未签发状态 — 没有记录 = 未签发

expiryDate 字段保留但不实现自动过期逻辑。管理员手动吊销。

### D-03: API 路径使用新实体命名

不沿用 gap-analysis 中的 `/admin/entry-permission/*`，改用：
- `POST /admin/enterprise-admission/{enterpriseId}/issue` — 签发准入证书
- `DELETE /admin/enterprise-admission/{enterpriseId}` — 吊销准入证书
- `GET /admin/enterprise-admission` — 查询准入证书列表
- `GET /admin/enterprise-admission/my` — 企业查看自身证书（ENTERPRISE 角色）

审核员资格证端点：
- `POST /admin/reviewer-qualification/{reviewerId}/issue` — 签发资格证
- `DELETE /admin/reviewer-qualification/{reviewerId}` — 吊销资格证
- `GET /admin/reviewer-qualification` — 查询资格证列表
- `GET /admin/reviewer-qualification/my` — 审核员查看自身资格证（REVIEWER 角色）

### D-04: 证书编号自动生成

- EnterpriseAdmission: `EA-{yyyyMMdd}-{6位随机数字}`，系统生成，管理员不可覆盖，UNIQUE 约束
- ReviewerQualification: 已有 `certificateNo` 字段（VARCHAR 50, UNIQUE），同样自动生成: `RQ-{yyyyMMdd}-{6位随机数字}`

### D-05: 前端页面结构

- 新增管理员页面 `/admin/certificates` — 管理员签发/吊销准入证书和审核员资格证（双 Tab 或分区）
- 企业用户在企业首页或个人中心查看自身准入证书状态
- 审核员在审核员首页或个人中心查看自身资格证状态
- 不复用现有 `/admin/verify/list`（那是碳报告审核）

### D-06: Flyway 迁移

- V4 迁移创建 `enterprise_admission` 表
- `reviewer_qualification` 表已存在（V1），无需新建
- 可选：V4 同时为 `reviewer_qualification` 添加唯一约束（如缺失）

### D-07: 重复签发防护

- 同一企业已有 ACTIVE 状态的准入证书时，拒绝重复签发（返回错误提示）
- 同一审核员已有 ACTIVE 状态的资格证时，拒绝重复签发
- 已吊销的证书可以重新签发（创建新记录）

### D-08: 吊销逻辑

- 吊销 = 将 status 从 1(有效) 更新为 2(已吊销)
- 只能吊销状态为 ACTIVE 的证书
- 吊销后记录保留（软删除标记不变，仅 status 变化）

### Claude's Discretion

- EnterpriseAdmission 实体的具体字段命名（遵循项目 Lombok @Data/@Builder 风格）
- 前端页面具体布局（遵循现有 admin views 的 Element Plus 风格）
- 证书列表的分页/筛选参数
- 是否在 AdminController 中新增端点 vs 创建独立的 CertificateController

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Backend - Existing Entities & Repositories
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/EntryPermission.java` — API 访问控制实体（NOT 准入证书）
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/ReviewerQualification.java` — 审核员资格证实体（reviewerId, qualificationType, certificateNo, issuingAuthority, issuedDate, expiryDate, status）
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/BaseEntity.java` — id, createdAt, updatedAt, deleted 软删除基类
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/EntryPermissionRepository.java` — API 权限查询
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/ReviewerQualificationRepository.java` — findByReviewerIdAndDeletedFalse, existsByCertificateNoAndDeletedFalse

### Backend - Admin Controller
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/AdminController.java` — 6 endpoints (users, dashboard, statistics, config, permissions), class-level @PreAuthorize("hasRole('ADMIN')")

### Backend - Database
- `oaiss-chain-backend/src/main/resources/db/migration/V1__init_schema.sql` — entry_permission 表（API权限）和 reviewer_qualification 表已存在
- `oaiss-chain-backend/src/main/resources/db/migration/V2__seed_data.sql` — 种子数据

### Backend - Pattern Reference
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonService.java` — Service 层模式参考
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/ApiResponse.java` — 统一响应格式

### Frontend - Admin Views
- `oaiss-chain-frontend/src/views/admin/SystemUsers.vue` — 管理员用户管理页面模式参考
- `oaiss-chain-frontend/src/views/admin/VerifyList.vue` — 碳报告审核页面（NOT 证书管理）
- `oaiss-chain-frontend/src/api/admin.ts` — 管理员 API 客户端（getUserList, updateUserStatus, getStatistics）
- `oaiss-chain-frontend/src/config/menu.ts` — 菜单配置，ADMIN 菜单项
- `oaiss-chain-frontend/src/router/index.ts` — 路由配置，admin 路由组

### Requirements & Gap Analysis
- `.planning/REQUIREMENTS.md` — REQ-07, REQ-08
- `docs/gap-analysis.md` — GAP-06 (签发准入证书), GAP-07 (签发审核员资格证)
- `docs/product-specification.md` §4.15 — 管理后台模块接口列表

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `AdminController` 已有 @PreAuthorize("hasRole('ADMIN')") 类级注解 — 新端点自动继承
- `ReviewerQualificationRepository` 已有 findByReviewerIdAndDeletedFalse 和 existsByCertificateNoAndDeletedFalse — 直接可用
- `BaseEntity` 提供 id, createdAt, updatedAt, deleted 软删除 — 新实体继承即可
- `ApiResponse.success(data)` 统一响应格式 — 所有新端点使用
- `admin.ts` API 客户端模式 — 新增函数遵循相同风格

### What Needs to Be Created
- `EnterpriseAdmission` entity (新)
- `EnterpriseAdmissionRepository` (新)
- `EnterpriseAdmissionService` (新)
- `ReviewerQualificationService` (新 — 现有 Repository 可用，但无 Service)
- AdminController 新增 8 个端点（4 准入 + 4 资格证）
- Flyway V4 迁移（enterprise_admission 表）
- 前端：admin/CertificateManage.vue (新)
- 前端：enterprise/EnterpriseHome.vue 或 profile 区域增加证书状态展示
- 前端：auditor/AuditorHome.vue 或 profile 区域增加资格证状态展示
- 前端：api/admin.ts 新增证书 API 函数
- 前端：router + menu 配置更新

### Role Access Matrix (Phase 10)
| Endpoint | ENTERPRISE | REVIEWER | ADMIN |
|----------|-----------|----------|-------|
| POST /admin/enterprise-admission/{id}/issue | NO | NO | YES |
| DELETE /admin/enterprise-admission/{id} | NO | NO | YES |
| GET /admin/enterprise-admission | NO | NO | YES |
| GET /admin/enterprise-admission/my | YES | NO | NO |
| POST /admin/reviewer-qualification/{id}/issue | NO | NO | YES |
| DELETE /admin/reviewer-qualification/{id} | NO | NO | YES |
| GET /admin/reviewer-qualification | NO | NO | YES |
| GET /admin/reviewer-qualification/my | NO | YES | NO |

### Known Gaps (record, do not fix)
- `EntryPermission` 实体语义混乱 — API 权限表命名为 "EntryPermission" 容易与准入证书混淆。不在本阶段重命名。
- 产品规格 §4.15 未列出准入证书/资格证的具体 API 端点 — 以 gap-analysis 为准（调整后的路径）

</code_context>

<specifics>
## Specific Ideas

- EnterpriseAdmission 实体参考 ReviewerQualification 的结构：id, createdAt, updatedAt, deleted, enterpriseId, certificateNo, issuedDate, status
- 证书签发时自动设置 issuedDate = LocalDate.now()
- 证书列表查询支持按 status 筛选、按 enterpriseId/reviewerId 筛选
- 管理员页面使用 Element Plus 的 el-tabs 分为 "准入证书" 和 "审核员资格证" 两个 Tab
- 企业/审核员的证书状态展示使用 el-tag 组件（绿色=有效，红色=已吊销，灰色=未签发）
- 未签发状态 = 数据库中无记录，前端通过 API 返回空结果判断

</specifics>

<deferred>
## Deferred Ideas

- EntryPermission 实体重命名 — 语义混乱但不影响功能，defer to v2
- 证书过期自动处理 — 当前仅手动吊销，自动过期逻辑 defer to v2
- 证书模板/PDF 导出 — 非核心功能，defer to v2
- 证书变更历史记录 — 当前仅保留最新状态，审计日志 defer to v2

</deferred>

---

*Phase: 10-admission-qualification*
*Context gathered: 2026-05-15*

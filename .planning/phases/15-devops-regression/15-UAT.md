---
status: complete
phase: 15-devops-regression
source: [13-01-SUMMARY.md, 13-02-SUMMARY.md, 13-03-SUMMARY.md, 14-02-SUMMARY.md, 15-01-SUMMARY.md, 15-02-SUMMARY.md, 15-03-SUMMARY.md]
started: 2026-05-21T15:30:00+08:00
updated: 2026-05-21T21:50:00+08:00
note: v2.0 milestone final UAT — covers Phase 13 (security hardening), Phase 14 (code quality), Phase 15 (DevOps)
---

## Current Test

[testing complete]

## Tests

### 1. docker-compose 无硬编码凭据
expected: |
  打开 docker-compose.yml 和 docker-compose.infra.yml。
  所有密码/密钥字段使用 ${VARIABLE} 形式，无任何硬编码值。
  具体检查：DB_PASSWORD, JWT_SECRET, REDIS_PASSWORD, MINIO_ACCESS_KEY, MINIO_SECRET_KEY — 均无 fallback 默认值（如 :-minioadmin 或 :-123456）。
result: pass
verified_by: automated — grep shows all fields use ${VAR} with no :- fallbacks in both files

### 2. FileController 角色授权
expected: |
  REVIEWER 角色调用 POST /api/v1/files/upload 应返回 403 Forbidden。
  ENTERPRISE 角色调用同一接口应成功（200）。
  （可用 curl 或 Swagger UI 验证，或检查 FileController.java 确认 @PreAuthorize 注解存在）
result: pass
verified_by: automated — class-level @PreAuthorize("isAuthenticated()") line 46, upload methods @PreAuthorize("hasAnyRole('ENTERPRISE','ADMIN')") lines 51/84/114/253/278, resolveUserId(JwtUserDetails) only

### 3. SearchController 需要认证
expected: |
  未携带 JWT token 调用 GET /api/v1/search?keyword=test 应返回 401 Unauthorized。
  （可用 curl 不带 Authorization header 验证，或检查 SearchController.java 确认 @PreAuthorize("isAuthenticated()") 存在）
result: pass
verified_by: automated — class-level @PreAuthorize("isAuthenticated()") at line 26

### 4. Prometheus 端点需要 ADMIN 角色
expected: |
  GET /actuator/prometheus 需要 ADMIN 角色。
  非 ADMIN 用户（如 ENTERPRISE）访问应返回 401/403。
  /actuator/health 仍然开放（无需认证）。
  （可检查 SecurityConfig.java 确认 actuator 白名单只保留 /actuator/health）
result: pass
verified_by: automated — SecurityConfig.java lines 80-82: /actuator/health/** permitAll, /actuator/prometheus hasRole("ADMIN"), /actuator/** authenticated

### 5. X-User-Id header 伪造已移除
expected: |
  JwtAuthenticationFilter.java 中不再有从 X-User-Id/X-User-Type header 提取用户信息的代码。
  FileController.java 的 resolveUserId() 方法只使用 JwtUserDetails 参数，无 HttpServletRequest 参数。
  （检查源码确认）
result: pass
verified_by: automated — JwtAuthenticationFilter.java: grep X-User-Id/X-User-Type returns nothing; FileController.java: resolveUserId(JwtUserDetails) only at line 378

### 6. Dev profile 使用 Flyway 管理 schema
expected: |
  oaiss-chain-backend/src/main/resources/application-dev.yml 包含：
  - spring.jpa.hibernate.ddl-auto: validate（不是 update）
  - spring.flyway.enabled: true
  （直接查看文件内容确认）
result: pass
verified_by: automated — application-dev.yml line 15: ddl-auto: validate, line 24: flyway.enabled: true

### 7. CI/CD 使用 Trivy 而非 OWASP
expected: |
  .github/workflows/ci-cd.yml (位于 oaiss-chain-backend/.github/) 中：
  - 不含 dependency-check-maven 字样
  - 含 trivy 或 aquasecurity/trivy-action 字样
  - 含 GitHub Secrets 使用说明注释
  （查看文件内容确认）
result: pass
verified_by: automated — ci-cd.yml line 118: aquasecurity/trivy-action@master, no dependency-check-maven, lines 5-8: GitHub Secrets documented (DOCKER_USERNAME, DOCKER_PASSWORD, SLACK_WEBHOOK)

### 8. E2E workflow 包含 MinIO 且无弱凭据
expected: |
  .github/workflows/e2e-tests.yml 中：
  - 含 minio: 服务定义
  - 不含 minioadmin 字样
  - 含 SPRING_PROFILES_ACTIVE: docker
  - 含 push: 触发器（main branch）
  （查看文件内容确认）
result: pass
verified_by: automated — e2e-tests.yml line 46: minio service, line 49-50: minio-test-access/minio-test-secret-key (no minioadmin), line 68: SPRING_PROFILES_ACTIVE: docker, line 4-5: push on main

### 9. RSA 私钥加密存储
expected: |
  src/main/java/com/oaiss/chain/config/RsaKeyMigrationRunner.java 存在。
  src/main/java/com/oaiss/chain/entity/RsaKeyPair.java 含 encrypted 字段（TINYINT）。
  db/migration/V7__encrypt_rsa_private_keys.sql 存在。
  （检查文件存在性和内容确认）
result: pass
verified_by: automated — RsaKeyMigrationRunner.java exists, RsaKeyPair.java line 83: @Column(name="encrypted"), V7__encrypt_rsa_private_keys.sql exists

### 10. v2.0 无新 E2E 回归
expected: |
  Phase 15 E2E 回归测试结论：Smoke 26/35 通过，所有失败均为预存在问题（auth fixture timeout、路由配置不匹配、ML 服务未启动）。
  Phase 13-14 的变更（分布式锁、凭据外部化、@PreAuthorize、Redis SCAN、异步缓存、RSA 加密）未引入新的测试失败。
  （参考 15-03-SUMMARY.md 和 15-VERIFICATION.md 的结论）
result: pass
verified_by: automated — 15-VERIFICATION.md confirms: verdict PASSED, Smoke 26/35 (74%), no v2.0 regressions, all failures pre-existing

## Summary

total: 10
passed: 10
issues: 0
pending: 0
skipped: 0

## Gaps

[none]

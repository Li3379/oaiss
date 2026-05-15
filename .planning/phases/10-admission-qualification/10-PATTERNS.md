# Phase 10: 准入与资格证 (Admission & Qualification Certificates) - Pattern Map

**Mapped:** 2026-05-15
**Files analyzed:** 11 (5 new backend, 2 new frontend, 4 modified)
**Analogs found:** 9 / 11

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `entity/EnterpriseAdmission.java` | model | CRUD | `entity/ReviewerQualification.java` | exact |
| `repository/EnterpriseAdmissionRepository.java` | repository | CRUD | `repository/ReviewerQualificationRepository.java` | exact |
| `service/EnterpriseAdmissionService.java` | service | CRUD | `service/CarbonService.java` | role-match |
| `service/ReviewerQualificationService.java` | service | CRUD | `service/CarbonService.java` | role-match |
| `controller/AdminController.java` (modify) | controller | request-response | existing file | exact |
| `db/migration/V4__enterprise_admission.sql` | migration | transform | `V1__init_schema.sql` | exact |
| `views/admin/CertificateManage.vue` | component | request-response | `views/admin/SystemUsers.vue` | exact |
| `api/admin.ts` (modify) | service | request-response | existing file | exact |
| `router/index.ts` (modify) | config | transform | existing file | exact |
| `config/menu.ts` (modify) | config | transform | existing file | exact |
| `views/enterprise/EnterpriseHome.vue` (modify) | component | request-response | `views/admin/SystemUsers.vue` | partial |

## Pattern Assignments

### `entity/EnterpriseAdmission.java` (model, CRUD)

**Analog:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/ReviewerQualification.java`

**Imports pattern** (lines 1-4):
```java
package com.oaiss.chain.entity;

import jakarta.persistence.*;
import lombok.*;
```

**Entity structure** (lines 1-63) -- copy the full shape:
```java
@Entity
@Table(name = "reviewer_qualification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewerQualification extends BaseEntity {

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "certificate_no", nullable = false, length = 50)
    private String certificateNo;

    @Column(name = "issued_date")
    private java.time.LocalDate issuedDate;

    @Column(name = "expiry_date")
    private java.time.LocalDate expiryDate;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private Integer status = 1;
}
```

**BaseEntity inheritance** (`entity/BaseEntity.java` lines 1-48):
```java
@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;
}
```

**Key decisions for EnterpriseAdmission:**
- Extend `BaseEntity` (inherits id, createdAt, updatedAt, deleted)
- Fields: `enterpriseId` (Long, NOT NULL), `certificateNo` (String, VARCHAR 50, UNIQUE), `issuedDate` (LocalDate), `expiryDate` (LocalDate), `status` (Integer, default 1)
- Table name: `enterprise_admission`
- Use `@Builder.Default` for status = 1

---

### `repository/EnterpriseAdmissionRepository.java` (repository, CRUD)

**Analog:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/ReviewerQualificationRepository.java`

**Full file** (lines 1-22):
```java
package com.oaiss.chain.repository;

import com.oaiss.chain.entity.ReviewerQualification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewerQualificationRepository extends JpaRepository<ReviewerQualification, Long> {

    List<ReviewerQualification> findByReviewerIdAndDeletedFalse(Long reviewerId);

    List<ReviewerQualification> findByReviewerIdAndStatusAndDeletedFalse(Long reviewerId, Integer status);

    boolean existsByCertificateNoAndDeletedFalse(String certificateNo);
}
```

**EnterpriseAdmissionRepository should define:**
- `List<EnterpriseAdmission> findByEnterpriseIdAndDeletedFalse(Long enterpriseId)`
- `List<EnterpriseAdmission> findByEnterpriseIdAndStatusAndDeletedFalse(Long enterpriseId, Integer status)`
- `boolean existsByCertificateNoAndDeletedFalse(String certificateNo)`
- `Optional<EnterpriseAdmission> findByEnterpriseIdAndStatusAndDeletedFalse(Long enterpriseId, Integer status)` -- for duplicate ACTIVE check

---

### `service/EnterpriseAdmissionService.java` (service, CRUD)

**Analog:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonService.java`

**Imports pattern** (lines 1-27):
```java
package com.oaiss.chain.service;

import com.oaiss.chain.dto.ApiResponse;
import com.oaiss.chain.entity.Enterprise;
import com.oaiss.chain.exception.BusinessException;
import com.oaiss.chain.repository.EnterpriseAdmissionRepository;
import com.oaiss.chain.repository.EnterpriseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
```

**Service class skeleton** (lines 36-39):
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseAdmissionService {
    private final EnterpriseAdmissionRepository enterpriseAdmissionRepository;
    private final EnterpriseRepository enterpriseRepository;
```

**Issue certificate pattern** -- adapted from CarbonService.createReport (lines 53-96):
```java
@Transactional
public EnterpriseAdmission issueCertificate(Long enterpriseId) {
    // 1. Verify enterprise exists
    Enterprise enterprise = enterpriseRepository.findById(enterpriseId)
            .orElseThrow(() -> BusinessException.notFound("error.enterprise.notFound"));

    // 2. Check duplicate ACTIVE (D-07)
    boolean hasActive = enterpriseAdmissionRepository
            .findByEnterpriseIdAndStatusAndDeletedFalse(enterpriseId, 1)
            .stream().findAny().isPresent();
    if (hasActive) {
        throw BusinessException.of(ErrorCode.PARAM_ERROR, "error.admission.alreadyActive");
    }

    // 3. Generate certificateNo: EA-{yyyyMMdd}-{6-digit-random}
    String certificateNo = String.format("EA-%s-%06d",
            java.time.format.DateTimeFormatter.BYY_MM_DD.format(LocalDate.now()),
            (int)(Math.random() * 1000000));

    // 4. Build and save
    EnterpriseAdmission admission = EnterpriseAdmission.builder()
            .enterpriseId(enterpriseId)
            .certificateNo(certificateNo)
            .issuedDate(LocalDate.now())
            .status(1) // ACTIVE
            .build();
    admission = enterpriseAdmissionRepository.save(admission);
    log.info("Enterprise admission issued: {} for enterprise {}", certificateNo, enterpriseId);
    return admission;
}
```

**Revoke certificate pattern** -- adapted from CarbonService.updateStatus:
```java
@Transactional
public void revokeCertificate(Long enterpriseId) {
    EnterpriseAdmission admission = enterpriseAdmissionRepository
            .findByEnterpriseIdAndStatusAndDeletedFalse(enterpriseId, 1)
            .stream().findFirst()
            .orElseThrow(() -> BusinessException.notFound("error.admission.notFound"));
    admission.setStatus(2); // REVOKED
    enterpriseAdmissionRepository.save(admission);
    log.info("Enterprise admission revoked for enterprise {}", enterpriseId);
}
```

**List certificates pattern** -- adapted from CarbonService.listReports (lines 198-203):
```java
public Page<EnterpriseAdmission> listCertificates(Integer status, Integer page, Integer size) {
    Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    if (status != null) {
        return enterpriseAdmissionRepository.findByStatusAndDeletedFalse(status, pageable);
    }
    return enterpriseAdmissionRepository.findByDeletedFalse(pageable);
}
```

**My certificate pattern** -- adapted from CarbonService.listMyReports (lines 208-223):
```java
public List<EnterpriseAdmission> getMyCertificate(Long enterpriseId) {
    return enterpriseAdmissionRepository.findByEnterpriseIdAndDeletedFalse(enterpriseId);
}
```

---

### `service/ReviewerQualificationService.java` (service, CRUD)

**Analog:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonService.java`

Same patterns as EnterpriseAdmissionService above, but using `ReviewerQualificationRepository` and `reviewerId` instead of `enterpriseId`.

**Key differences:**
- CertificateNo prefix: `RQ-{yyyyMMdd}-{6-digit-random}` instead of `EA-`
- Uses `reviewerId` field
- Repository already exists: `ReviewerQualificationRepository` (lines 1-22 of that file)
- Validation: check `findByReviewerIdAndStatusAndDeletedFalse(reviewerId, 1)` for duplicate ACTIVE

**Note:** ReviewerQualification entity already exists (V1 migration), no new entity or migration needed. The service just wraps the existing repository with issue/revoke/list/my logic.

---

### `controller/AdminController.java` (controller, request-response) -- MODIFY

**Analog:** existing file `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/AdminController.java`

**Existing class annotations** (lines 33-38):
```java
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "08. 管理后台", description = "管理员后台管理接口，包括用户管理、系统监控、数据统计等")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
```

**Existing endpoint pattern -- list with pagination** (lines 43-81):
```java
@GetMapping("/users")
@Operation(
    summary = "查询用户列表",
    description = "分页查询系统用户列表。支持按用户类型、状态筛选。",
    security = @SecurityRequirement(name = "Bearer Authentication")
)
@ApiResponses(value = {
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200", description = "查询成功"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403", description = "无权限，仅管理员可访问")
})
public ApiResponse<Page<User>> listUsers(
        @Parameter(description = "页码，从1开始", example = "1")
        @RequestParam(defaultValue = "1") Integer page,
        @Parameter(description = "每页数量", example = "10")
        @RequestParam(defaultValue = "10") Integer size) {
    Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    // ... query logic ...
    return ApiResponse.success(users);
}
```

**Existing error handling pattern** (lines 110-117):
```java
User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));
```

**New endpoints to add (8 total):**

1. `POST /admin/enterprise-admission/{enterpriseId}/issue` -- calls `enterpriseAdmissionService.issueCertificate(enterpriseId)`, returns `ApiResponse.success(admission)`
2. `DELETE /admin/enterprise-admission/{enterpriseId}` -- calls `enterpriseAdmissionService.revokeCertificate(enterpriseId)`, returns `ApiResponse.success()`
3. `GET /admin/enterprise-admission` -- calls `enterpriseAdmissionService.listCertificates(status, page, size)`, returns `ApiResponse.success(admissions)`
4. `GET /admin/enterprise-admission/my` -- uses `@AuthenticationPrincipal JwtUserDetails`, calls `enterpriseAdmissionService.getMyCertificate(enterpriseId)`, returns `ApiResponse.success(list)`. NOTE: This endpoint needs `@PreAuthorize("hasRole('ENTERPRISE')")` at method level to override class-level ADMIN.
5-8. Same 4 endpoints for reviewer-qualification using `reviewerQualificationService`

**Critical pattern for `/my` endpoints** -- override class-level auth:
```java
@GetMapping("/enterprise-admission/my")
@PreAuthorize("hasRole('ENTERPRISE')")
@Operation(summary = "查看自身准入证书", ...)
public ApiResponse<List<EnterpriseAdmission>> getMyAdmission(
        @AuthenticationPrincipal JwtUserDetails currentUser) {
    Enterprise enterprise = enterpriseRepository.findByUserId(currentUser.getUserId())
            .orElseThrow(() -> BusinessException.notFound("error.enterprise.notFound"));
    return ApiResponse.success(enterpriseAdmissionService.getMyCertificate(enterprise.getId()));
}
```

---

### `db/migration/V4__enterprise_admission.sql` (migration, transform)

**Analog:** `oaiss-chain-backend/src/main/resources/db/migration/V1__init_schema.sql`

**Table creation pattern** (adapted from reviewer_qualification, lines 91-104):
```sql
CREATE TABLE `enterprise_admission` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `created_at`     DATETIME     NOT NULL,
    `updated_at`     DATETIME     NOT NULL,
    `is_deleted`     TINYINT(1)   NOT NULL DEFAULT 0,
    `enterprise_id`  BIGINT       NOT NULL,
    `certificate_no` VARCHAR(50)  NOT NULL,
    `issued_date`    DATE         NULL,
    `expiry_date`    DATE         NULL,
    `status`         INT          NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_enterprise_admission_certificate_no` (`certificate_no`),
    INDEX `idx_enterprise_admission_enterprise_id` (`enterprise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Key conventions from V1:**
- All tables use `BIGINT NOT NULL AUTO_INCREMENT` for `id`
- All tables include `created_at`, `updated_at`, `is_deleted` (from BaseEntity)
- VARCHAR lengths match JPA `@Column(length=...)` definitions
- UNIQUE constraints use `uk_{table}_{column}` naming
- INDEX uses `idx_{table}_{column}` naming

---

### `views/admin/CertificateManage.vue` (component, request-response)

**Analog:** `oaiss-chain-frontend/src/views/admin/SystemUsers.vue`

**Script setup pattern** (lines 1-113):
```vue
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEnterpriseAdmissionList, revokeEnterpriseAdmission,
         getReviewerQualificationList, revokeReviewerQualification,
         issueEnterpriseAdmission, issueReviewerQualification } from '../../api/admin'

const { t } = useI18n()
```

**Data fetching pattern** (lines 41-58):
```typescript
const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      ...(searchForm.status !== '' && { status: searchForm.status }),
    }
    const response = await getEnterpriseAdmissionList(params)
    dataList.value = response.items || []
    total.value = response.total || 0
  } catch (error) {
    ElMessage.error(t('certificateManage.loadFailed'))
  } finally {
    loading.value = false
  }
}
```

**Status toggle with confirmation** (lines 76-101):
```typescript
const handleRevoke = async (row) => {
  try {
    await ElMessageBox.confirm(
      t('certificateManage.confirmRevoke'),
      t('certificateManage.revokeTitle'),
      { confirmButtonText: t('common.confirm'), cancelButtonText: t('common.cancel'), type: 'warning' }
    )
    await revokeEnterpriseAdmission(row.enterpriseId)
    ElMessage.success(t('certificateManage.revokeSuccess'))
    fetchData()
  } catch (error) {
    if (error === 'cancel') { /* user cancelled */ }
  }
}
```

**Template pattern with el-tabs** (adapted from SystemUsers.vue):
```vue
<template>
  <section class="certificate-page">
    <el-card class="section-card" shadow="never">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>{{ t('menu.admin') }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ t('menu.certificateManage') }}</el-breadcrumb-item>
      </el-breadcrumb>
    </el-card>

    <el-card class="section-card" shadow="never">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane :label="t('certificateManage.admission')" name="admission">
          <!-- admission table + pagination -->
        </el-tab-pane>
        <el-tab-pane :label="t('certificateManage.qualification')" name="qualification">
          <!-- qualification table + pagination -->
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </section>
</template>
```

**Status tag pattern** (lines 103-109):
```typescript
const getStatusType = (status) => {
  return status === 1 ? 'success' : 'danger'  // ACTIVE=green, REVOKED=red
}
const getStatusText = (status) => {
  return status === 1 ? t('certificateManage.active') : t('certificateManage.revoked')
}
```

**Pagination pattern** (lines 171-183):
```vue
<div class="pagination-row">
  <el-pagination
    v-model:current-page="currentPage"
    v-model:page-size="pageSize"
    background
    :page-sizes="[10, 20, 50]"
    layout="total, sizes, prev, pager, next, jumper"
    :total="total"
    @size-change="onSizeChange"
    @current-change="onCurrentChange"
  />
</div>
```

**Style pattern** (lines 187-209):
```vue
<style scoped>
.certificate-page { display: flex; flex-direction: column; gap: 14px; }
.section-card { border: 1px solid var(--border-color); border-radius: 12px; }
.search-form { display: flex; flex-wrap: wrap; }
.pagination-row { margin-top: 14px; display: flex; justify-content: flex-end; }
</style>
```

---

### `api/admin.ts` (service, request-response) -- MODIFY

**Analog:** existing file `oaiss-chain-frontend/src/api/admin.ts`

**Existing pattern** (lines 1-15):
```typescript
import request from './request'
import type { PageRequest } from '../types'

export function getUserList(params?: PageRequest): Promise<unknown> {
  return request.get('/admin/users', { params })
}

export function updateUserStatus(userId: number, status: number): Promise<void> {
  return request.put(`/admin/users/${userId}/status`, null, { params: { status } })
}
```

**New functions to add:**
```typescript
// Enterprise Admission
export function getEnterpriseAdmissionList(params?: PageRequest): Promise<unknown> {
  return request.get('/admin/enterprise-admission', { params })
}

export function issueEnterpriseAdmission(enterpriseId: number): Promise<unknown> {
  return request.post(`/admin/enterprise-admission/${enterpriseId}/issue`)
}

export function revokeEnterpriseAdmission(enterpriseId: number): Promise<void> {
  return request.delete(`/admin/enterprise-admission/${enterpriseId}`)
}

export function getMyEnterpriseAdmission(): Promise<unknown> {
  return request.get('/admin/enterprise-admission/my')
}

// Reviewer Qualification
export function getReviewerQualificationList(params?: PageRequest): Promise<unknown> {
  return request.get('/admin/reviewer-qualification', { params })
}

export function issueReviewerQualification(reviewerId: number): Promise<unknown> {
  return request.post(`/admin/reviewer-qualification/${reviewerId}/issue`)
}

export function revokeReviewerQualification(reviewerId: number): Promise<void> {
  return request.delete(`/admin/reviewer-qualification/${reviewerId}`)
}

export function getMyReviewerQualification(): Promise<unknown> {
  return request.get('/admin/reviewer-qualification/my')
}
```

---

### `router/index.ts` (config, transform) -- MODIFY

**Analog:** existing file, admin route group (lines 131-155)

**Pattern to follow:**
```typescript
{
  path: 'admin/system/users',
  name: 'AdminSystemUsers',
  component: () => import('../views/admin/SystemUsers.vue'),
  meta: { title: '用户管理', roles: [ROLE.ADMIN] },
},
```

**New route to add:**
```typescript
{
  path: 'admin/certificates',
  name: 'AdminCertificates',
  component: () => import('../views/admin/CertificateManage.vue'),
  meta: { title: '证书管理', roles: [ROLE.ADMIN] },
},
```

---

### `config/menu.ts` (config, transform) -- MODIFY

**Analog:** existing file, ADMIN menu section (lines 73-87)

**Existing pattern:**
```typescript
[ROLE.ADMIN]: [
  {
    label: 'menu.admin',
    children: [
      { label: 'menu.systemManage', children: [
        { label: 'menu.userManage', path: '/admin/system/users' },
        { label: 'menu.carbonManage', path: '/admin/system/carbon' },
        { label: 'menu.systemConfig', path: '/admin/system/config' },
      ] },
      { label: 'menu.dataManage', children: [{ label: 'menu.statisticsData', path: '/admin/data/statistics' }] },
      { label: 'menu.certificationManage', children: [{ label: 'menu.certificationList', path: '/admin/verify/list' }] },
    ],
  },
],
```

**Add to admin children:**
```typescript
{ label: 'menu.certificateManage', children: [{ label: 'menu.certificateList', path: '/admin/certificates' }] },
```

---

## Shared Patterns

### Entity Base Class
**Source:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/BaseEntity.java`
**Apply to:** EnterpriseAdmission entity
- Extends `BaseEntity` to inherit id, createdAt, updatedAt, deleted
- Uses `@MappedSuperclass` + `@EntityListeners(AuditingEntityListener.class)`
- JPA auditing auto-populates createdAt/updatedAt

### Unified API Response
**Source:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/ApiResponse.java`
**Apply to:** All new AdminController endpoints
- `ApiResponse.success()` -- no data
- `ApiResponse.success(data)` -- with data
- `ApiResponse.error(code, message)` -- error response
- All endpoints return `ApiResponse<T>`

### Business Exception
**Source:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/exception/BusinessException.java`
**Apply to:** All new services
- `BusinessException.notFound(messageKey)` -- resource not found (code 1002)
- `BusinessException.of(code, messageKey, args)` -- custom error
- `BusinessException.paramError(messageKey)` -- validation error (code 1001)

### Error Codes
**Source:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/constant/ErrorCode.java`
**Apply to:** New certificate-related errors
- Use `PARAM_ERROR` (1001) for duplicate certificate check
- Use `RESOURCE_NOT_FOUND` (1002) for missing enterprise/reviewer/admission
- Consider adding new codes in the 7xxx range for certificate module if needed

### Frontend Request Interceptor
**Source:** `oaiss-chain-frontend/src/api/request.ts`
**Apply to:** All new API calls
- Interceptor auto-transforms `pageNum/pageSize` to `page/size` (lines 44-53)
- Interceptor auto-unwraps `ApiResponse.data` and transforms Spring `Page` to `{ items, total, page, size, totalPages }` (lines 108-118)
- API functions should use `Promise<unknown>` return type and pass `params` directly
- Error handling is centralized in interceptor -- no try/catch needed in API layer

### Frontend Type Definitions
**Source:** `oaiss-chain-frontend/src/types/api.ts`
**Apply to:** New API response types
- `ApiResponse<T>` -- standard envelope with code, message, data, meta
- `SpringPage<T>` -- Spring Data Page shape (content, totalElements, etc.)
- `PageRequest` -- frontend pagination params (pageNum, pageSize, etc.)
- `PageResponse<T>` -- transformed pagination response (list, total, pageNum, pageSize, pages)

### Frontend Admin Page Pattern
**Source:** `oaiss-chain-frontend/src/views/admin/SystemUsers.vue`
**Apply to:** CertificateManage.vue
- `<script setup lang="ts">` with `useI18n()`
- `reactive()` for search form, `ref()` for data/pagination
- `onMounted(() => fetchData())` initial load
- `el-table` with `v-loading`, `el-tag` for status, `el-pagination` for paging
- `ElMessageBox.confirm()` before destructive actions
- `ElMessage.success()` / `ElMessage.error()` for feedback
- `el-breadcrumb` navigation header
- `el-card` with `shadow="never"` and `section-card` class

---

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `views/enterprise/EnterpriseHome.vue` (modify) | component | request-response | No existing enterprise home with cert status; partial match from SystemUsers.vue for el-tag display pattern |
| `views/auditor/AuditorHome.vue` (modify) | component | request-response | No existing auditor home with cert status; same partial match |

**Recommendation:** For enterprise/auditor certificate status display, use `el-tag` with green (success) for ACTIVE, red (danger) for REVOKED, gray (info) for "未签发" (no record). Fetch via `getMyEnterpriseAdmission()` / `getMyReviewerQualification()` API calls. If empty array returned, display "未签发" state.

---

## Metadata

**Analog search scope:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/` (entity, repository, service, controller, dto, exception, constant), `oaiss-chain-backend/src/main/resources/db/migration/`, `oaiss-chain-frontend/src/` (api, views/admin, config, router, types)
**Files scanned:** 12 analog files
**Pattern extraction date:** 2026-05-15

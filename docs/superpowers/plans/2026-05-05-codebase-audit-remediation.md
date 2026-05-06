# OAISS CHAIN 代码审计修复实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补齐缺失测试、提升前后端覆盖率至 80%+，并完成 TypeScript 迁移和 i18n 国际化

**Architecture:** 分 6 阶段执行：P0 后端 Controller 测试 → P1 前端核心页面单元测试 → P1 E2E 测试 → P2 前端剩余页面测试 → P3 TypeScript 迁移 → P3 i18n 国际化。每阶段独立可交付，遵循 TDD 流程。

**Tech Stack:** Spring Boot 3.2.5, JUnit 5, MockMvc, Mockito, Vue 3, Vitest, @vue/test-utils, Playwright, Pinia, Element Plus

---

## Phase 1: P0 — 补齐缺失的后端 Controller 测试

### Task 1: ThirdPartyControllerTest

**Files:**
- Create: `oaiss-chain-backend/src/test/java/com/oaiss/chain/controller/ThirdPartyControllerTest.java`
- Reference: `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/ThirdPartyController.java`
- Reference: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/ThirdPartyService.java`

- [ ] **Step 1: 创建测试文件骨架**

```java
package com.oaiss.chain.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaiss.chain.entity.ThirdPartyOrg;
import com.oaiss.chain.entity.CarbonReport;
import com.oaiss.chain.service.ThirdPartyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ThirdPartyController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class ThirdPartyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ThirdPartyService thirdPartyService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "testUser", "password",
                List.of(new SimpleGrantedAuthority("ROLE_THIRD_PARTY"))
            )
        );
    }
}
```

- [ ] **Step 2: 运行测试确认骨架编译通过**

Run: `cd oaiss-chain-backend && mvn test -pl . -Dtest=ThirdPartyControllerTest -DfailIfNoTests=false`
Expected: BUILD SUCCESS (tests pass vacuously)

- [ ] **Step 3: 添加 GET /third-party/org-info 测试**

在 `ThirdPartyControllerTest.java` 中添加：

```java
@Test
@DisplayName("GET /third-party/org-info — 返回第三方机构信息")
void getOrgInfo_returnsOrgInfo() throws Exception {
    ThirdPartyOrg org = new ThirdPartyOrg();
    org.setId(1L);
    org.setOrgName("GreenVerify Inc.");
    when(thirdPartyService.getOrgInfo("testUser")).thenReturn(org);

    mockMvc.perform(get("/api/v1/third-party/org-info"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.orgName").value("GreenVerify Inc."));

    verify(thirdPartyService, times(1)).getOrgInfo("testUser");
}
```

- [ ] **Step 4: 添加 GET /third-party/carbon-reports 测试**

```java
@Test
@DisplayName("GET /third-party/carbon-reports — 返回碳报告分页列表")
void getCarbonReports_returnsPage() throws Exception {
    CarbonReport report = new CarbonReport();
    report.setId(1L);
    report.setEnterpriseId(10L);
    Page<CarbonReport> page = new PageImpl<>(List.of(report));
    when(thirdPartyService.getCarbonReports(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(page);

    mockMvc.perform(get("/api/v1/third-party/carbon-reports")
            .param("page", "1")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.content[0].enterpriseId").value(10));

    verify(thirdPartyService, times(1))
        .getCarbonReports(isNull(), isNull(), isNull(), eq(1), eq(10));
}
```

- [ ] **Step 5: 添加 GET /third-party/statistics 测试**

```java
@Test
@DisplayName("GET /third-party/statistics — 返回统计数据")
void getStatistics_returnsMap() throws Exception {
    Map<String, Object> stats = Map.of("totalReports", 42, "pendingReviews", 5);
    when(thirdPartyService.getStatistics("testUser")).thenReturn(stats);

    mockMvc.perform(get("/api/v1/third-party/statistics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.totalReports").value(42));

    verify(thirdPartyService, times(1)).getStatistics("testUser");
}
```

- [ ] **Step 6: 添加 PUT /third-party/contact 测试**

```java
@Test
@DisplayName("PUT /third-party/contact — 更新联系人信息")
void updateContact_success() throws Exception {
    doNothing().when(thirdPartyService).updateContact(anyString(), any(), any());

    mockMvc.perform(put("/api/v1/third-party/contact")
            .param("contactPerson", "张三")
            .param("contactPhone", "13800138000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));

    verify(thirdPartyService, times(1))
        .updateContact(eq("testUser"), eq("张三"), eq("13800138000"));
}
```

- [ ] **Step 7: 运行全部测试确认通过**

Run: `cd oaiss-chain-backend && mvn test -pl . -Dtest=ThirdPartyControllerTest`
Expected: Tests run: 4, Failures: 0, Errors: 0

- [ ] **Step 8: Commit**

```bash
git add oaiss-chain-backend/src/test/java/com/oaiss/chain/controller/ThirdPartyControllerTest.java
git commit -m "test: add ThirdPartyControllerTest covering 4 endpoints"
```

---

### Task 2: CarbonNeutralProjectControllerTest

**Files:**
- Create: `oaiss-chain-backend/src/test/java/com/oaiss/chain/controller/CarbonNeutralProjectControllerTest.java`
- Reference: `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/CarbonNeutralProjectController.java`
- Reference: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonNeutralProjectService.java`

- [ ] **Step 1: 创建测试文件骨架**

```java
package com.oaiss.chain.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaiss.chain.dto.CarbonNeutralProjectRequest;
import com.oaiss.chain.dto.CarbonNeutralProjectResponse;
import com.oaiss.chain.dto.ProjectVerificationRequest;
import com.oaiss.chain.service.CarbonNeutralProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = CarbonNeutralProjectController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class CarbonNeutralProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CarbonNeutralProjectService carbonNeutralProjectService;

    private CarbonNeutralProjectResponse sampleResponse;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "enterprise1", "password",
                List.of(new SimpleGrantedAuthority("ROLE_ENTERPRISE"))
            )
        );
        sampleResponse = new CarbonNeutralProjectResponse();
        sampleResponse.setId(1L);
        sampleResponse.setProjectName("碳汇造林项目");
        sampleResponse.setStatus("DRAFT");
    }

    private void setAdminAuth() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin", "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            )
        );
    }
}
```

- [ ] **Step 2: 运行测试确认骨架编译通过**

Run: `cd oaiss-chain-backend && mvn test -pl . -Dtest=CarbonNeutralProjectControllerTest -DfailIfNoTests=false`
Expected: BUILD SUCCESS

- [ ] **Step 3: 添加 CRUD 端点测试（POST, PUT, GET, GET /search, GET /my）**

```java
@Test
@DisplayName("POST /carbon-neutral — 创建碳中和项目")
void createProject_success() throws Exception {
    CarbonNeutralProjectRequest request = new CarbonNeutralProjectRequest();
    request.setProjectName("碳汇造林项目");
    request.setProjectType("AFFORESTATION");
    when(carbonNeutralProjectService.createProject(any(), anyString()))
        .thenReturn(sampleResponse);

    mockMvc.perform(post("/api/v1/carbon-neutral")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.projectName").value("碳汇造林项目"));

    verify(carbonNeutralProjectService, times(1)).createProject(any(), eq("enterprise1"));
}

@Test
@DisplayName("PUT /carbon-neutral/{id} — 更新碳中和项目")
void updateProject_success() throws Exception {
    CarbonNeutralProjectRequest request = new CarbonNeutralProjectRequest();
    request.setProjectName("更新后的项目名");
    when(carbonNeutralProjectService.updateProject(eq(1L), any(), anyString()))
        .thenReturn(sampleResponse);

    mockMvc.perform(put("/api/v1/carbon-neutral/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
}

@Test
@DisplayName("GET /carbon-neutral/{id} — 获取项目详情")
void getProject_success() throws Exception {
    when(carbonNeutralProjectService.getProject(1L)).thenReturn(sampleResponse);

    mockMvc.perform(get("/api/v1/carbon-neutral/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(1));
}

@Test
@DisplayName("GET /carbon-neutral/search — 搜索项目列表")
void searchProjects_returnsPage() throws Exception {
    Page<CarbonNeutralProjectResponse> page = new PageImpl<>(List.of(sampleResponse));
    when(carbonNeutralProjectService.searchProjects(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(page);

    mockMvc.perform(get("/api/v1/carbon-neutral/search")
            .param("page", "1").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray());
}

@Test
@DisplayName("GET /carbon-neutral/my — 获取我的项目列表")
void getMyProjects_returnsPage() throws Exception {
    Page<CarbonNeutralProjectResponse> page = new PageImpl<>(List.of(sampleResponse));
    when(carbonNeutralProjectService.getMyProjects(anyString(), any(), anyInt(), anyInt()))
        .thenReturn(page);

    mockMvc.perform(get("/api/v1/carbon-neutral/my")
            .param("page", "1").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray());
}
```

- [ ] **Step 4: 添加流程端点测试（submit, review, start, submit-verification）**

```java
@Test
@DisplayName("POST /carbon-neutral/{id}/submit — 提交项目审核")
void submitProject_success() throws Exception {
    when(carbonNeutralProjectService.submitProject(1L, "enterprise1"))
        .thenReturn(sampleResponse);

    mockMvc.perform(post("/api/v1/carbon-neutral/1/submit"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
}

@Test
@DisplayName("POST /carbon-neutral/{id}/review — 审核项目")
void reviewProject_success() throws Exception {
    setAdminAuth();
    when(carbonNeutralProjectService.reviewProject(eq(1L), any(), anyString()))
        .thenReturn(sampleResponse);

    mockMvc.perform(post("/api/v1/carbon-neutral/1/review")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("approved", true, "comment", "通过"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
}

@Test
@DisplayName("POST /carbon-neutral/{id}/start — 启动项目")
void startProject_success() throws Exception {
    when(carbonNeutralProjectService.startProject(1L, "enterprise1"))
        .thenReturn(sampleResponse);

    mockMvc.perform(post("/api/v1/carbon-neutral/1/start"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
}

@Test
@DisplayName("POST /carbon-neutral/{id}/submit-verification — 提交验证")
void submitVerification_success() throws Exception {
    when(carbonNeutralProjectService.submitVerification(eq(1L), any(), anyString()))
        .thenReturn(sampleResponse);

    mockMvc.perform(post("/api/v1/carbon-neutral/1/submit-verification")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("verifierId", 2L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
}
```

- [ ] **Step 5: 添加验证/认证端点测试（verify, use-credits, monitoring, apply-certification, certify, terminate）**

```java
@Test
@DisplayName("POST /carbon-neutral/verify — 验证项目")
void verifyProject_success() throws Exception {
    setAdminAuth();
    ProjectVerificationRequest request = new ProjectVerificationRequest();
    request.setProjectId(1L);
    request.setApproved(true);
    when(carbonNeutralProjectService.verifyProject(any(), anyString()))
        .thenReturn(sampleResponse);

    mockMvc.perform(post("/api/v1/carbon-neutral/verify")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
}

@Test
@DisplayName("POST /carbon-neutral/{id}/use-credits — 使用碳信用")
void useCredits_success() throws Exception {
    when(carbonNeutralProjectService.useCredits(eq(1L), any(BigDecimal.class), anyString()))
        .thenReturn(sampleResponse);

    mockMvc.perform(post("/api/v1/carbon-neutral/1/use-credits")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("amount", new BigDecimal("100.00")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
}

@Test
@DisplayName("PUT /carbon-neutral/{id}/monitoring — 更新监测数据")
void updateMonitoring_success() throws Exception {
    when(carbonNeutralProjectService.updateMonitoring(eq(1L), any(), anyString()))
        .thenReturn(sampleResponse);

    mockMvc.perform(put("/api/v1/carbon-neutral/1/monitoring")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("monitoringData", "数据"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
}

@Test
@DisplayName("POST /carbon-neutral/{id}/apply-certification — 申请认证")
void applyCertification_success() throws Exception {
    when(carbonNeutralProjectService.applyCertification(eq(1L), any(), anyString()))
        .thenReturn(sampleResponse);

    mockMvc.perform(post("/api/v1/carbon-neutral/1/apply-certification")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("certOrg", "中国质量认证中心"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
}

@Test
@DisplayName("POST /carbon-neutral/{id}/certify — 颁发认证")
void certify_success() throws Exception {
    setAdminAuth();
    when(carbonNeutralProjectService.certify(eq(1L), any(), anyString()))
        .thenReturn(sampleResponse);

    mockMvc.perform(post("/api/v1/carbon-neutral/1/certify")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("certNo", "CERT-2026-001"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
}

@Test
@DisplayName("POST /carbon-neutral/{id}/terminate — 终止项目")
void terminate_success() throws Exception {
    when(carbonNeutralProjectService.terminate(eq(1L), any(), anyString()))
        .thenReturn(sampleResponse);

    mockMvc.perform(post("/api/v1/carbon-neutral/1/terminate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("reason", "项目调整"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
}
```

- [ ] **Step 6: 添加查询端点测试（GET /pending-verification）**

```java
@Test
@DisplayName("GET /carbon-neutral/pending-verification — 获取待验证项目")
void getPendingVerification_returnsPage() throws Exception {
    setAdminAuth();
    Page<CarbonNeutralProjectResponse> page = new PageImpl<>(List.of(sampleResponse));
    when(carbonNeutralProjectService.getPendingVerification(anyString(), anyInt(), anyInt()))
        .thenReturn(page);

    mockMvc.perform(get("/api/v1/carbon-neutral/pending-verification")
            .param("page", "1").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray());
}
```

- [ ] **Step 7: 运行全部测试确认通过**

Run: `cd oaiss-chain-backend && mvn test -pl . -Dtest=CarbonNeutralProjectControllerTest`
Expected: Tests run: 16, Failures: 0, Errors: 0

- [ ] **Step 8: Commit**

```bash
git add oaiss-chain-backend/src/test/java/com/oaiss/chain/controller/CarbonNeutralProjectControllerTest.java
git commit -m "test: add CarbonNeutralProjectControllerTest covering 15 endpoints"
```

---

## Phase 2: P1 — 前端核心页面单元测试（Top 5）

### Task 3: Login.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/Login.test.js`
- Reference: `oaiss-chain-frontend/src/views/Login.vue`
- Reference: `oaiss-chain-frontend/src/api/auth.js`
- Reference: `oaiss-chain-frontend/src/store/index.js`

- [ ] **Step 1: 创建 Login 测试骨架**

```javascript
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

// Mock API module
vi.mock('../../api/auth.js', () => ({
  login: vi.fn(),
  getCaptcha: vi.fn(() => Promise.resolve({ data: { captchaId: '1', captchaImage: 'base64...' } }))
}))

// Mock router
const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush })
}))

// Mock Element Plus
vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn() }
}))

import Login from '../Login.vue'
import * as authApi from '../../api/auth.js'
import { ElMessage } from 'element-plus'

describe('Login.vue', () => {
  let pinia

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.clearAllMocks()
    localStorage.clear()
    sessionStorage.clear()
  })

  function mountLogin() {
    return mount(Login, {
      global: {
        plugins: [pinia],
        stubs: {
          'el-form': { template: '<form><slot /></form>' },
          'el-form-item': { template: '<div><slot /></div>' },
          'el-input': { template: '<input />', props: ['modelValue'] },
          'el-button': { template: '<button @click="$emit(\'click\')"><slot /></button>' },
          'el-radio-group': { template: '<div><slot /></div>' },
          'el-radio': { template: '<div><slot /></div>' },
          'el-dialog': { template: '<div><slot /></div>', props: ['modelValue'] },
          'el-image': { template: '<div />' }
        }
      }
    })
  }
})
```

- [ ] **Step 2: 运行确认骨架通过**

Run: `cd oaiss-chain-frontend && npx vitest run src/views/__tests__/Login.test.js`
Expected: PASS (0 tests)

- [ ] **Step 3: 添加登录成功测试**

```javascript
it('登录成功后跳转到首页', async () => {
  authApi.login.mockResolvedValue({
    data: {
      token: 'eyJhbGciOiJIUzI1NiJ9.' + btoa(JSON.stringify({ sub: 'user1', role: 'ENTERPRISE', userId: 1, enterpriseId: 10 })) + '.sig',
      refreshToken: 'refresh-token'
    }
  })

  const wrapper = mountLogin()
  // Simulate form submission
  const form = wrapper.findComponent({ name: 'ElForm' }) || wrapper.find('form')
  await form.trigger('submit')

  // Verify API called
  expect(authApi.login).toHaveBeenCalled()
})
```

- [ ] **Step 4: 添加登录失败测试**

```javascript
it('登录失败显示错误消息', async () => {
  authApi.login.mockRejectedValue(new Error('用户名或密码错误'))

  const wrapper = mountLogin()
  const form = wrapper.find('form')
  await form.trigger('submit')

  expect(authApi.login).toHaveBeenCalled()
})
```

- [ ] **Step 5: 添加角色切换测试**

```javascript
it('切换角色标签更新登录类型', async () => {
  const wrapper = mountLogin()
  // Verify role switching UI exists
  const radioGroup = wrapper.findComponent({ name: 'ElRadioGroup' }) || wrapper.find('[role="radiogroup"]')
  expect(radioGroup.exists() || wrapper.find('.login-tabs').exists()).toBe(true)
})
```

- [ ] **Step 6: 运行测试确认通过**

Run: `cd oaiss-chain-frontend && npx vitest run src/views/__tests__/Login.test.js`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add oaiss-chain-frontend/src/views/__tests__/Login.test.js
git commit -m "test: add Login.vue unit tests for auth flow"
```

---

### Task 4: CarbonUpload.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/CarbonUpload.test.js`
- Reference: `oaiss-chain-frontend/src/views/enterprise/CarbonUpload.vue`
- Reference: `oaiss-chain-frontend/src/api/carbon.js`

- [ ] **Step 1: 创建测试骨架**

```javascript
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('../../api/carbon.js', () => ({
  uploadReport: vi.fn(),
  getMyReports: vi.fn(() => Promise.resolve({ data: { content: [], totalElements: 0 } })),
  getReportDetail: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
  ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) }
}))

import CarbonUpload from '../enterprise/CarbonUpload.vue'
import * as carbonApi from '../../api/carbon.js'

describe('CarbonUpload.vue', () => {
  let pinia

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.clearAllMocks()
  })

  function mountComponent() {
    return mount(CarbonUpload, {
      global: {
        plugins: [pinia],
        stubs: {
          'el-table': { template: '<table><slot /></table>' },
          'el-table-column': { template: '<td><slot /></td>' },
          'el-form': { template: '<form><slot /></form>' },
          'el-form-item': { template: '<div><slot /></div>' },
          'el-input': { template: '<input />' },
          'el-select': { template: '<select />' },
          'el-option': { template: '<option />' },
          'el-button': { template: '<button><slot /></button>' },
          'el-upload': { template: '<div><slot /></div>' },
          'el-pagination': { template: '<div />' },
          'el-dialog': { template: '<div><slot /></div>', props: ['modelValue'] },
          'el-tag': { template: '<span><slot /></span>' }
        }
      }
    })
  }
})
```

- [ ] **Step 2: 运行确认骨架通过**

Run: `cd oaiss-chain-frontend && npx vitest run src/views/__tests__/CarbonUpload.test.js`
Expected: PASS

- [ ] **Step 3: 添加页面加载测试**

```javascript
it('页面加载时获取报告列表', async () => {
  mountComponent()
  // Wait for onMounted
  await vi.dynamicImportSettled()
  expect(carbonApi.getMyReports).toHaveBeenCalled()
})
```

- [ ] **Step 4: 添加上传成功测试**

```javascript
it('上传报告成功后刷新列表', async () => {
  carbonApi.uploadReport.mockResolvedValue({ data: { id: 1 } })
  carbonApi.getMyReports.mockResolvedValue({
    data: { content: [{ id: 1, reportName: '测试报告' }], totalElements: 1 }
  })

  const wrapper = mountComponent()
  // Trigger upload via component method or button click
  expect(wrapper.exists()).toBe(true)
})
```

- [ ] **Step 5: 运行测试确认通过**

Run: `cd oaiss-chain-frontend && npx vitest run src/views/__tests__/CarbonUpload.test.js`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add oaiss-chain-frontend/src/views/__tests__/CarbonUpload.test.js
git commit -m "test: add CarbonUpload.vue unit tests"
```

---

### Task 5: TradingMarket.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/TradingMarket.test.js`
- Reference: `oaiss-chain-frontend/src/views/enterprise/TradingMarket.vue`
- Reference: `oaiss-chain-frontend/src/api/trade.js`

- [ ] **Step 1: 创建测试骨架并添加核心测试**

```javascript
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('../../api/trade.js', () => ({
  getTradeList: vi.fn(() => Promise.resolve({ data: { content: [], totalElements: 0 } })),
  createTrade: vi.fn(),
  getTradeDetail: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn() }
}))

import TradingMarket from '../enterprise/TradingMarket.vue'

describe('TradingMarket.vue', () => {
  let pinia

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.clearAllMocks()
  })

  function mountComponent() {
    return mount(TradingMarket, {
      global: {
        plugins: [pinia],
        stubs: {
          'el-table': { template: '<table><slot /></table>' },
          'el-table-column': { template: '<td><slot /></td>' },
          'el-button': { template: '<button><slot /></button>' },
          'el-form': { template: '<form><slot /></form>' },
          'el-form-item': { template: '<div><slot /></div>' },
          'el-input': { template: '<input />' },
          'el-select': { template: '<select />' },
          'el-pagination': { template: '<div />' },
          'el-dialog': { template: '<div><slot /></div>', props: ['modelValue'] },
          'el-tag': { template: '<span><slot /></span>' },
          'el-descriptions': { template: '<div><slot /></div>' },
          'el-descriptions-item': { template: '<div><slot /></div>' }
        }
      }
    })
  }

  it('页面加载时获取交易列表', async () => {
    const { getTradeList } = await import('../../api/trade.js')
    mountComponent()
    await vi.dynamicImportSettled()
    expect(getTradeList).toHaveBeenCalled()
  })

  it('组件正确渲染', () => {
    const wrapper = mountComponent()
    expect(wrapper.exists()).toBe(true)
  })
})
```

- [ ] **Step 2: 运行测试确认通过**

Run: `cd oaiss-chain-frontend && npx vitest run src/views/__tests__/TradingMarket.test.js`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add oaiss-chain-frontend/src/views/__tests__/TradingMarket.test.js
git commit -m "test: add TradingMarket.vue unit tests"
```

---

### Task 6: AuditList.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/AuditList.test.js`
- Reference: `oaiss-chain-frontend/src/views/auditor/AuditList.vue`
- Reference: `oaiss-chain-frontend/src/api/carbon.js`

- [ ] **Step 1: 创建测试骨架并添加核心测试**

```javascript
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('../../api/carbon.js', () => ({
  getPendingReviews: vi.fn(() => Promise.resolve({ data: { content: [], totalElements: 0 } })),
  reviewReport: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn() },
  ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) }
}))

import AuditList from '../auditor/AuditList.vue'

describe('AuditList.vue', () => {
  let pinia

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.clearAllMocks()
  })

  function mountComponent() {
    return mount(AuditList, {
      global: {
        plugins: [pinia],
        stubs: {
          'el-table': { template: '<table><slot /></table>' },
          'el-table-column': { template: '<td><slot /></td>' },
          'el-button': { template: '<button><slot /></button>' },
          'el-tag': { template: '<span><slot /></span>' },
          'el-pagination': { template: '<div />' },
          'el-dialog': { template: '<div><slot /></div>', props: ['modelValue'] },
          'el-form': { template: '<form><slot /></form>' },
          'el-form-item': { template: '<div><slot /></div>' },
          'el-input': { template: '<textarea />' }
        }
      }
    })
  }

  it('页面加载时获取待审核列表', async () => {
    const { getPendingReviews } = await import('../../api/carbon.js')
    mountComponent()
    await vi.dynamicImportSettled()
    expect(getPendingReviews).toHaveBeenCalled()
  })

  it('组件正确渲染', () => {
    const wrapper = mountComponent()
    expect(wrapper.exists()).toBe(true)
  })
})
```

- [ ] **Step 2: 运行测试确认通过**

Run: `cd oaiss-chain-frontend && npx vitest run src/views/__tests__/AuditList.test.js`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add oaiss-chain-frontend/src/views/__tests__/AuditList.test.js
git commit -m "test: add AuditList.vue unit tests"
```

---

### Task 7: SystemUsers.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/SystemUsers.test.js`
- Reference: `oaiss-chain-frontend/src/views/admin/SystemUsers.vue`
- Reference: `oaiss-chain-frontend/src/api/admin.js`

- [ ] **Step 1: 创建测试骨架并添加核心测试**

```javascript
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('../../api/admin.js', () => ({
  getUserList: vi.fn(() => Promise.resolve({ data: { content: [], totalElements: 0 } })),
  createUser: vi.fn(),
  updateUser: vi.fn(),
  deleteUser: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn() },
  ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) }
}))

import SystemUsers from '../admin/SystemUsers.vue'

describe('SystemUsers.vue', () => {
  let pinia

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.clearAllMocks()
  })

  function mountComponent() {
    return mount(SystemUsers, {
      global: {
        plugins: [pinia],
        stubs: {
          'el-table': { template: '<table><slot /></table>' },
          'el-table-column': { template: '<td><slot /></td>' },
          'el-button': { template: '<button><slot /></button>' },
          'el-form': { template: '<form><slot /></form>' },
          'el-form-item': { template: '<div><slot /></div>' },
          'el-input': { template: '<input />' },
          'el-select': { template: '<select />' },
          'el-option': { template: '<option />' },
          'el-pagination': { template: '<div />' },
          'el-dialog': { template: '<div><slot /></div>', props: ['modelValue'] },
          'el-tag': { template: '<span><slot /></span>' }
        }
      }
    })
  }

  it('页面加载时获取用户列表', async () => {
    const { getUserList } = await import('../../api/admin.js')
    mountComponent()
    await vi.dynamicImportSettled()
    expect(getUserList).toHaveBeenCalled()
  })

  it('组件正确渲染', () => {
    const wrapper = mountComponent()
    expect(wrapper.exists()).toBe(true)
  })
})
```

- [ ] **Step 2: 运行测试确认通过**

Run: `cd oaiss-chain-frontend && npx vitest run src/views/__tests__/SystemUsers.test.js`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add oaiss-chain-frontend/src/views/__tests__/SystemUsers.test.js
git commit -m "test: add SystemUsers.vue unit tests"
```

---

## Phase 3: P1 — 补充 E2E 测试

### Task 8: ThirdParty 监控流程 E2E

**Files:**
- Create: `oaiss-chain-frontend/tests/e2e/d6-third-party-monitor.spec.js`
- Reference: `oaiss-chain-frontend/tests/e2e/fixtures/auth.js`
- Reference: `oaiss-chain-frontend/tests/e2e/d5-admin-users.spec.js`

- [ ] **Step 1: 创建 E2E 测试**

```javascript
import { test, expect } from '@playwright/test'
import { buildStorageState, setupApiMock } from './fixtures/auth.js'

const THIRD_PARTY_STORAGE = buildStorageState('thirdParty')

test.describe('ThirdParty 监控流程', () => {
  test.use({ storageState: THIRD_PARTY_STORAGE })

  test.beforeEach(async ({ page }) => {
    await setupApiMock(page, {
      'GET /api/v1/third-party/org-info': {
        code: 200,
        data: { id: 1, orgName: 'GreenVerify Inc.', contactPerson: '张三', contactPhone: '13800138000' }
      },
      'GET /api/v1/third-party/carbon-reports': {
        code: 200,
        data: {
          content: [
            { id: 1, enterpriseId: 10, enterpriseName: '测试企业', reportName: '2025年度碳排放报告', status: 'PENDING' },
            { id: 2, enterpriseId: 11, enterpriseName: '绿色科技', reportName: '2025年度碳排放报告', status: 'APPROVED' }
          ],
          totalElements: 2, totalPages: 1, number: 0, size: 10
        }
      },
      'GET /api/v1/third-party/statistics': {
        code: 200,
        data: { totalReports: 42, pendingReviews: 5, approvedReports: 35, rejectedReports: 2 }
      }
    })
    await page.goto('/third-party/monitor')
  })

  test('显示机构信息', async ({ page }) => {
    await expect(page.getByText('GreenVerify Inc.')).toBeVisible()
  })

  test('显示碳报告列表', async ({ page }) => {
    await expect(page.getByText('测试企业')).toBeVisible()
    await expect(page.getByText('绿色科技')).toBeVisible()
  })

  test('显示统计数据', async ({ page }) => {
    await expect(page.getByText('42')).toBeVisible()
  })

  test('更新联系人信息', async ({ page }) => {
    await setupApiMock(page, {
      'PUT /api/v1/third-party/contact': { code: 200, data: null }
    })

    const contactInput = page.getByPlaceholder(/联系人/)
    if (await contactInput.isVisible()) {
      await contactInput.fill('李四')
      await page.getByRole('button', { name: /保存|更新/ }).click()
      await expect(page.getByText(/成功/)).toBeVisible()
    }
  })
})
```

- [ ] **Step 2: 运行 E2E 测试**

Run: `cd oaiss-chain-frontend && npx playwright test tests/e2e/d6-third-party-monitor.spec.js`
Expected: 4 passed

- [ ] **Step 3: Commit**

```bash
git add oaiss-chain-frontend/tests/e2e/d6-third-party-monitor.spec.js
git commit -m "test: add ThirdParty monitor E2E spec"
```

---

### Task 9: CarbonNeutral 项目全流程 E2E

**Files:**
- Create: `oaiss-chain-frontend/tests/e2e/d7-carbon-neutral.spec.js`
- Reference: `oaiss-chain-frontend/tests/e2e/fixtures/auth.js`

- [ ] **Step 1: 创建 E2E 测试**

```javascript
import { test, expect } from '@playwright/test'
import { buildStorageState, setupApiMock } from './fixtures/auth.js'

const ENTERPRISE_STORAGE = buildStorageState('enterprise')
const ADMIN_STORAGE = buildStorageState('admin')

test.describe('CarbonNeutral 项目全流程', () => {
  test.describe('企业用户', () => {
    test.use({ storageState: ENTERPRISE_STORAGE })

    test.beforeEach(async ({ page }) => {
      await setupApiMock(page, {
        'GET /api/v1/carbon-neutral/my': {
          code: 200,
          data: {
            content: [
              { id: 1, projectName: '碳汇造林项目', status: 'DRAFT', projectType: 'AFFORESTATION' }
            ],
            totalElements: 1, totalPages: 1, number: 0, size: 10
          }
        },
        'POST /api/v1/carbon-neutral': {
          code: 200,
          data: { id: 2, projectName: '新能源项目', status: 'DRAFT' }
        },
        'POST /api/v1/carbon-neutral/1/submit': {
          code: 200,
          data: { id: 1, projectName: '碳汇造林项目', status: 'PENDING_REVIEW' }
        }
      })
      await page.goto('/enterprise/carbon-neutral')
    })

    test('显示我的项目列表', async ({ page }) => {
      await expect(page.getByText('碳汇造林项目')).toBeVisible()
    })

    test('创建新项目', async ({ page }) => {
      await page.getByRole('button', { name: /新建|创建/ }).click()
      await page.getByPlaceholder(/项目名称/).fill('新能源项目')
      await page.getByRole('button', { name: /确定|保存/ }).click()
      await expect(page.getByText(/成功/)).toBeVisible()
    })

    test('提交项目审核', async ({ page }) => {
      const submitBtn = page.getByRole('button', { name: /提交/ })
      if (await submitBtn.isVisible()) {
        await submitBtn.click()
        await expect(page.getByText(/成功/)).toBeVisible()
      }
    })
  })

  test.describe('管理员审核', () => {
    test.use({ storageState: ADMIN_STORAGE })

    test.beforeEach(async ({ page }) => {
      await setupApiMock(page, {
        'GET /api/v1/carbon-neutral/search': {
          code: 200,
          data: {
            content: [
              { id: 1, projectName: '碳汇造林项目', status: 'PENDING_REVIEW' }
            ],
            totalElements: 1, totalPages: 1, number: 0, size: 10
          }
        },
        'POST /api/v1/carbon-neutral/1/review': {
          code: 200,
          data: { id: 1, projectName: '碳汇造林项目', status: 'APPROVED' }
        }
      })
      await page.goto('/admin/carbon-neutral')
    })

    test('审核通过项目', async ({ page }) => {
      await expect(page.getByText('碳汇造林项目')).toBeVisible()
      const approveBtn = page.getByRole('button', { name: /通过|批准/ })
      if (await approveBtn.isVisible()) {
        await approveBtn.click()
        await page.getByRole('button', { name: /确定|确认/ }).click()
        await expect(page.getByText(/成功/)).toBeVisible()
      }
    })
  })
})
```

- [ ] **Step 2: 运行 E2E 测试**

Run: `cd oaiss-chain-frontend && npx playwright test tests/e2e/d7-carbon-neutral.spec.js`
Expected: 4 passed

- [ ] **Step 3: Commit**

```bash
git add oaiss-chain-frontend/tests/e2e/d7-carbon-neutral.spec.js
git commit -m "test: add CarbonNeutral project lifecycle E2E spec"
```

---

### Task 10: CreditScore 查询流程 E2E

**Files:**
- Create: `oaiss-chain-frontend/tests/e2e/d8-credit-score.spec.js`
- Reference: `oaiss-chain-frontend/tests/e2e/fixtures/auth.js`

- [ ] **Step 1: 创建 E2E 测试**

```javascript
import { test, expect } from '@playwright/test'
import { buildStorageState, setupApiMock } from './fixtures/auth.js'

const ENTERPRISE_STORAGE = buildStorageState('enterprise')

test.describe('CreditScore 查询流程', () => {
  test.use({ storageState: ENTERPRISE_STORAGE })

  test.beforeEach(async ({ page }) => {
    await setupApiMock(page, {
      'GET /api/v1/credit/score': {
        code: 200,
        data: { enterpriseId: 10, score: 85, level: 'A', factors: ['交易活跃度高', '碳排放达标'] }
      },
      'GET /api/v1/credit/history': {
        code: 200,
        data: {
          content: [
            { id: 1, eventType: 'TRADE', scoreChange: 5, description: '完成碳交易', createdAt: '2026-04-01' },
            { id: 2, eventType: 'REPORT', scoreChange: 3, description: '提交碳报告', createdAt: '2026-03-15' }
          ],
          totalElements: 2, totalPages: 1, number: 0, size: 10
        }
      }
    })
    await page.goto('/enterprise/credit-score')
  })

  test('显示信用评分', async ({ page }) => {
    await expect(page.getByText('85')).toBeVisible()
    await expect(page.getByText('A')).toBeVisible()
  })

  test('显示评分因素', async ({ page }) => {
    await expect(page.getByText('交易活跃度高')).toBeVisible()
  })

  test('显示信用历史', async ({ page }) => {
    await expect(page.getByText('完成碳交易')).toBeVisible()
    await expect(page.getByText('提交碳报告')).toBeVisible()
  })
})
```

- [ ] **Step 2: 运行 E2E 测试**

Run: `cd oaiss-chain-frontend && npx playwright test tests/e2e/d8-credit-score.spec.js`
Expected: 3 passed

- [ ] **Step 3: Commit**

```bash
git add oaiss-chain-frontend/tests/e2e/d8-credit-score.spec.js
git commit -m "test: add CreditScore query E2E spec"
```

---

## Phase 4: P2 — 前端剩余页面单元测试

> 每个 Task 遵循与 Task 3-7 相同的模式：mock API → mount with stubs → 测试加载/渲染/交互。以下仅列出差异部分。

### Task 11: OrdersManage.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/OrdersManage.test.js`
- Reference: `oaiss-chain-frontend/src/views/enterprise/OrdersManage.vue`
- Mock: `trade.js` → `getMyOrders`, `cancelOrder`

- [ ] **Step 1: 创建测试文件，测试页面加载和订单列表渲染**
- [ ] **Step 2: 运行确认通过**
- [ ] **Step 3: Commit**

---

### Task 12: TradingP2P.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/TradingP2P.test.js`
- Reference: `oaiss-chain-frontend/src/views/enterprise/TradingP2P.vue`
- Mock: `trade.js` → `createP2PTrade`, `getTradeList`

- [ ] **Step 1: 创建测试文件，测试 P2P 交易表单和列表**
- [ ] **Step 2: 运行确认通过**
- [ ] **Step 3: Commit**

---

### Task 13: CompanyDashboard.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/CompanyDashboard.test.js`
- Reference: `oaiss-chain-frontend/src/views/enterprise/CompanyDashboard.vue`
- Mock: `carbon.js`, `trade.js`, `carbonCoin.js`

- [ ] **Step 1: 创建测试文件，测试仪表盘数据加载**
- [ ] **Step 2: 运行确认通过**
- [ ] **Step 3: Commit**

---

### Task 14: CreditScore.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/CreditScore.test.js`
- Reference: `oaiss-chain-frontend/src/views/enterprise/CreditScore.vue`
- Mock: `credit.js` → `getScore`, `getHistory`

- [ ] **Step 1: 创建测试文件**
- [ ] **Step 2: 运行确认通过**
- [ ] **Step 3: Commit**

---

### Task 15: CarbonCoin.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/CarbonCoin.test.js`
- Reference: `oaiss-chain-frontend/src/views/enterprise/CarbonCoin.vue`
- Mock: `carbonCoin.js` → `getBalance`, `getTransactions`, `transfer`

- [ ] **Step 1: 创建测试文件**
- [ ] **Step 2: 运行确认通过**
- [ ] **Step 3: Commit**

---

### Task 16: Blockchain.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/Blockchain.test.js`
- Reference: `oaiss-chain-frontend/src/views/enterprise/Blockchain.vue`
- Mock: `blockchain.js` → `getBlocks`, `getBlockDetail`

- [ ] **Step 1: 创建测试文件**
- [ ] **Step 2: 运行确认通过**
- [ ] **Step 3: Commit**

---

### Task 17: CarbonNeutral.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/CarbonNeutral.test.js`
- Reference: `oaiss-chain-frontend/src/views/enterprise/CarbonNeutral.vue`
- Mock: `carbonNeutral.js` → `getMyProjects`, `createProject`

- [ ] **Step 1: 创建测试文件**
- [ ] **Step 2: 运行确认通过**
- [ ] **Step 3: Commit**

---

### Task 18: CarbonNeutralDetail.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/CarbonNeutralDetail.test.js`
- Reference: `oaiss-chain-frontend/src/views/enterprise/CarbonNeutralDetail.vue`
- Mock: `carbonNeutral.js` → `getProject`, `submitProject`, `startProject`

- [ ] **Step 1: 创建测试文件**
- [ ] **Step 2: 运行确认通过**
- [ ] **Step 3: Commit**

---

### Task 19: EmissionData.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/EmissionData.test.js`
- Reference: `oaiss-chain-frontend/src/views/enterprise/EmissionData.vue`
- Mock: `emission.js` → `getRatings`, `getRankings`

- [ ] **Step 1: 创建测试文件**
- [ ] **Step 2: 运行确认通过**
- [ ] **Step 3: Commit**

---

### Task 20: UserProfile.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/UserProfile.test.js`
- Reference: `oaiss-chain-frontend/src/views/enterprise/UserProfile.vue`
- Mock: `user.js` → `getProfile`, `updateProfile`

- [ ] **Step 1: 创建测试文件**
- [ ] **Step 2: 运行确认通过**
- [ ] **Step 3: Commit**

---

### Task 21: VerifyList.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/VerifyList.test.js`
- Reference: `oaiss-chain-frontend/src/views/authenticator/VerifyList.vue`
- Mock: `carbon.js` → `getPendingVerifications`, `verifyReport`

- [ ] **Step 1: 创建测试文件**
- [ ] **Step 2: 运行确认通过**
- [ ] **Step 3: Commit**

---

### Task 22: Monitor.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/Monitor.test.js`
- Reference: `oaiss-chain-frontend/src/views/thirdParty/Monitor.vue`
- Mock: `thirdParty.js` → `getOrgInfo`, `getStatistics`

- [ ] **Step 1: 创建测试文件**
- [ ] **Step 2: 运行确认通过**
- [ ] **Step 3: Commit**

---

### Task 23: SystemCarbon.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/SystemCarbon.test.js`
- Reference: `oaiss-chain-frontend/src/views/admin/SystemCarbon.vue`
- Mock: `admin.js` → `getCarbonList`, `updateCarbon`

- [ ] **Step 1: 创建测试文件**
- [ ] **Step 2: 运行确认通过**
- [ ] **Step 3: Commit**

---

### Task 24: SystemConfig.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/SystemConfig.test.js`
- Reference: `oaiss-chain-frontend/src/views/admin/SystemConfig.vue`
- Mock: `admin.js` → `getConfig`, `updateConfig`

- [ ] **Step 1: 创建测试文件**
- [ ] **Step 2: 运行确认通过**
- [ ] **Step 3: Commit**

---

### Task 25: DataStatistics.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/DataStatistics.test.js`
- Reference: `oaiss-chain-frontend/src/views/admin/DataStatistics.vue`
- Mock: `admin.js` → `getStatistics`

- [ ] **Step 1: 创建测试文件**
- [ ] **Step 2: 运行确认通过**
- [ ] **Step 3: Commit**

---

### Task 26: OfficialHome.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/OfficialHome.test.js`
- Reference: `oaiss-chain-frontend/src/views/OfficialHome.vue`

- [ ] **Step 1: 创建测试文件（纯展示页面，测试渲染）**
- [ ] **Step 2: 运行确认通过**
- [ ] **Step 3: Commit**

---

### Task 27: NotFound.vue 单元测试

**Files:**
- Create: `oaiss-chain-frontend/src/views/__tests__/NotFound.test.js`
- Reference: `oaiss-chain-frontend/src/views/NotFound.vue`

- [ ] **Step 1: 创建测试文件（测试 404 渲染和返回首页链接）**
- [ ] **Step 2: 运行确认通过**
- [ ] **Step 3: Commit**

---

## Phase 5: P3 — TypeScript 迁移

### Task 28: 工具链配置

**Files:**
- Create: `oaiss-chain-frontend/tsconfig.json`
- Create: `oaiss-chain-frontend/src/vite-env.d.ts`
- Modify: `oaiss-chain-frontend/vite.config.js`
- Modify: `oaiss-chain-frontend/package.json`

- [ ] **Step 1: 安装 TypeScript 依赖**

Run: `cd oaiss-chain-frontend && npm install -D typescript @types/node vue-tsc`
Expected: added 3 packages

- [ ] **Step 2: 创建 tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "strict": true,
    "jsx": "preserve",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "esModuleInterop": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "noEmit": true,
    "paths": {
      "@/*": ["./src/*"]
    },
    "types": ["vite/client"]
  },
  "include": ["src/**/*.ts", "src/**/*.d.ts", "src/**/*.tsx", "src/**/*.vue"],
  "exclude": ["node_modules", "dist"]
}
```

- [ ] **Step 3: 创建 vite-env.d.ts**

```typescript
/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
```

- [ ] **Step 4: Commit**

```bash
git add oaiss-chain-frontend/tsconfig.json oaiss-chain-frontend/src/vite-env.d.ts
git commit -m "chore: add TypeScript toolchain configuration"
```

---

### Task 29: 迁移 request.js → request.ts

**Files:**
- Rename: `oaiss-chain-frontend/src/api/request.js` → `request.ts`
- Reference: `oaiss-chain-frontend/src/api/request.js`

- [ ] **Step 1: 重命名并添加类型**

```typescript
import axios, { type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { getToken, removeTokens } from '@/utils/auth'

interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

const request: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // Translate pagination params
    if (config.params) {
      if (config.params.pageNum !== undefined) {
        config.params.page = config.params.pageNum
        delete config.params.pageNum
      }
      if (config.params.pageSize !== undefined) {
        config.params.size = config.params.pageSize
        delete config.params.pageSize
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      removeTokens()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default request
export type { ApiResponse }
```

- [ ] **Step 2: 运行构建确认无错误**

Run: `cd oaiss-chain-frontend && npx vue-tsc --noEmit`
Expected: no errors (or expected errors from unmigrated files)

- [ ] **Step 3: Commit**

```bash
git add oaiss-chain-frontend/src/api/request.ts
git rm oaiss-chain-frontend/src/api/request.js
git commit -m "refactor: migrate request.js to TypeScript"
```

---

### Task 30: 迁移 API 模块 → .ts

**Files:**
- Rename: 16 个 `src/api/*.js` → `.ts`
- Modify: 每个文件添加接口类型定义

- [ ] **Step 1: 迁移 auth.js → auth.ts**

```typescript
import request from './request'

interface LoginParams {
  username: string
  password: string
  captchaId?: string
  captchaCode?: string
  loginType?: string
}

interface LoginResponse {
  token: string
  refreshToken: string
}

interface CaptchaResponse {
  captchaId: string
  captchaImage: string
}

export function login(data: LoginParams) {
  return request.post<LoginResponse>('/auth/login', data)
}

export function register(data: any) {
  return request.post('/auth/register', data)
}

export function refreshToken(token: string) {
  return request.post('/auth/refresh', { refreshToken: token })
}

export function getCaptcha() {
  return request.get<CaptchaResponse>('/captcha/generate')
}

export function logout() {
  return request.post('/auth/logout')
}
```

- [ ] **Step 2: 迁移其余 15 个 API 模块**

每个模块遵循相同模式：导入 `request`，定义接口类型，导出带类型的函数。

- [ ] **Step 3: 运行构建确认无错误**

Run: `cd oaiss-chain-frontend && npx vue-tsc --noEmit`

- [ ] **Step 4: Commit**

```bash
git add oaiss-chain-frontend/src/api/
git commit -m "refactor: migrate all API modules to TypeScript"
```

---

### Task 31: 迁移 Store + Utils

**Files:**
- Rename: `src/store/index.js` → `index.ts`
- Rename: `src/utils/auth.js` → `auth.ts`
- Rename: 其余 utils 文件

- [ ] **Step 1: 迁移 store/index.js → index.ts**

为 state、getters、actions 添加类型注解。

- [ ] **Step 2: 迁移 utils/auth.js → auth.ts**

为所有函数添加参数和返回值类型。

- [ ] **Step 3: 运行构建确认**

Run: `cd oaiss-chain-frontend && npx vue-tsc --noEmit`

- [ ] **Step 4: Commit**

```bash
git add oaiss-chain-frontend/src/store/ oaiss-chain-frontend/src/utils/
git commit -m "refactor: migrate store and utils to TypeScript"
```

---

### Task 32: Vue 页面渐进迁移

**Files:**
- Modify: 22 个 `src/views/**/*.vue`

- [ ] **Step 1: 为所有 .vue 文件添加 `<script setup lang="ts">`**

将 `<script setup>` 改为 `<script setup lang="ts">`，为 ref/reactive 添加类型注解。

- [ ] **Step 2: 运行构建确认**

Run: `cd oaiss-chain-frontend && npx vue-tsc --noEmit`

- [ ] **Step 3: 运行全部单元测试确认无回归**

Run: `cd oaiss-chain-frontend && npx vitest run`

- [ ] **Step 4: Commit**

```bash
git add oaiss-chain-frontend/src/views/
git commit -m "refactor: migrate Vue pages to TypeScript script setup"
```

---

## Phase 6: P3 — i18n 国际化

### Task 33: 集成 vue-i18n

**Files:**
- Create: `oaiss-chain-frontend/src/i18n/index.js`
- Create: `oaiss-chain-frontend/src/i18n/zh-CN.json`
- Modify: `oaiss-chain-frontend/src/main.js`

- [ ] **Step 1: 安装 vue-i18n**

Run: `cd oaiss-chain-frontend && npm install vue-i18n@9`

- [ ] **Step 2: 创建 i18n 配置**

```javascript
import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN.json'
import enUS from './en-US.json'

const i18n = createI18n({
  legacy: false,
  locale: localStorage.getItem('language') || 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS
  }
})

export default i18n
```

- [ ] **Step 3: 在 main.js 中注册 i18n**

```javascript
import i18n from './i18n'
app.use(i18n)
```

- [ ] **Step 4: Commit**

```bash
git add oaiss-chain-frontend/src/i18n/ oaiss-chain-frontend/src/main.js
git commit -m "feat: integrate vue-i18n framework"
```

---

### Task 34: 提取中文语言包

**Files:**
- Create: `oaiss-chain-frontend/src/i18n/zh-CN.json`

- [ ] **Step 1: 扫描所有 .vue 文件提取中文字符串**

遍历所有 Vue 文件，提取硬编码中文到 `zh-CN.json`，按模块组织：

```json
{
  "common": {
    "search": "搜索",
    "reset": "重置",
    "add": "新增",
    "edit": "编辑",
    "delete": "删除",
    "confirm": "确定",
    "cancel": "取消",
    "save": "保存",
    "submit": "提交",
    "back": "返回",
    "loading": "加载中...",
    "noData": "暂无数据",
    "success": "操作成功",
    "error": "操作失败"
  },
  "login": {
    "title": "碳排放数据可信管理与交易系统",
    "username": "请输入用户名",
    "password": "请输入密码",
    "loginBtn": "登录",
    "registerBtn": "注册",
    "captcha": "请输入验证码"
  },
  "menu": {
    "dashboard": "企业总览",
    "carbonUpload": "碳数据上报",
    "tradingMarket": "交易市场",
    "ordersManage": "订单管理",
    "creditScore": "信用评分",
    "carbonCoin": "碳币管理",
    "blockchain": "区块链浏览器",
    "carbonNeutral": "碳中和项目",
    "userProfile": "个人中心"
  }
}
```

- [ ] **Step 2: 在组件中使用 `$t()` 替换硬编码中文**

示例：
```vue
<!-- Before -->
<el-button>搜索</el-button>

<!-- After -->
<el-button>{{ $t('common.search') }}</el-button>
```

- [ ] **Step 3: Commit**

```bash
git add oaiss-chain-frontend/src/i18n/zh-CN.json
git commit -m "feat: extract Chinese strings to zh-CN language pack"
```

---

### Task 35: 创建英文语言包 + 语言切换

**Files:**
- Create: `oaiss-chain-frontend/src/i18n/en-US.json`
- Create: `oaiss-chain-frontend/src/components/LanguageSwitch.vue`
- Modify: `oaiss-chain-frontend/src/layout/` (添加语言切换组件到顶栏)

- [ ] **Step 1: 创建英文语言包**

```json
{
  "common": {
    "search": "Search",
    "reset": "Reset",
    "add": "Add",
    "edit": "Edit",
    "delete": "Delete",
    "confirm": "Confirm",
    "cancel": "Cancel",
    "save": "Save",
    "submit": "Submit",
    "back": "Back",
    "loading": "Loading...",
    "noData": "No Data",
    "success": "Success",
    "error": "Error"
  },
  "login": {
    "title": "Carbon Emission Data Management & Trading System",
    "username": "Enter username",
    "password": "Enter password",
    "loginBtn": "Login",
    "registerBtn": "Register",
    "captcha": "Enter captcha"
  },
  "menu": {
    "dashboard": "Dashboard",
    "carbonUpload": "Carbon Upload",
    "tradingMarket": "Trading Market",
    "ordersManage": "Orders",
    "creditScore": "Credit Score",
    "carbonCoin": "Carbon Coin",
    "blockchain": "Blockchain Explorer",
    "carbonNeutral": "Carbon Neutral Projects",
    "userProfile": "Profile"
  }
}
```

- [ ] **Step 2: 创建语言切换组件**

```vue
<script setup>
import { useI18n } from 'vue-i18n'

const { locale } = useI18n()

function toggleLanguage() {
  const next = locale.value === 'zh-CN' ? 'en-US' : 'zh-CN'
  locale.value = next
  localStorage.setItem('language', next)
}
</script>

<template>
  <el-button text @click="toggleLanguage">
    {{ locale === 'zh-CN' ? 'English' : '中文' }}
  </el-button>
</template>
```

- [ ] **Step 3: 运行全部测试确认无回归**

Run: `cd oaiss-chain-frontend && npx vitest run`

- [ ] **Step 4: Commit**

```bash
git add oaiss-chain-frontend/src/i18n/en-US.json oaiss-chain-frontend/src/components/LanguageSwitch.vue
git commit -m "feat: add English language pack and language switch component"
```

---

## 最终验证

### Task 36: 全量验证

- [ ] **Step 1: 运行后端全量测试**

Run: `cd oaiss-chain-backend && mvn test`
Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 2: 运行前端单元测试**

Run: `cd oaiss-chain-frontend && npx vitest run`
Expected: 20+ test files, 100+ tests pass

- [ ] **Step 3: 运行前端 E2E 测试**

Run: `cd oaiss-chain-frontend && npx playwright test`
Expected: 8+ specs pass

- [ ] **Step 4: 运行前端构建**

Run: `cd oaiss-chain-frontend && npm run build`
Expected: Build successful

- [ ] **Step 5: 运行 TypeScript 类型检查**

Run: `cd oaiss-chain-frontend && npx vue-tsc --noEmit`
Expected: No errors

- [ ] **Step 6: Commit 最终状态**

```bash
git add -A
git commit -m "chore: complete codebase audit remediation — all phases"
```

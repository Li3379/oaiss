package com.oaiss.chain.aop;

import com.oaiss.chain.annotation.RequirePermission;
import com.oaiss.chain.entity.AccountPermissionList;
import com.oaiss.chain.entity.EntryPermission;
import com.oaiss.chain.exception.BusinessException;
import com.oaiss.chain.repository.AccountPermissionListRepository;
import com.oaiss.chain.repository.EntryPermissionRepository;
import com.oaiss.chain.security.JwtUserDetails;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 权限校验切面测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionAspectTest {

    @Mock
    private EntryPermissionRepository entryPermissionRepository;
    @Mock
    private AccountPermissionListRepository accountPermissionListRepository;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private PermissionAspect permissionAspect;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("权限校验 - 未登录抛出异常")
    void checkPermission_NotLoggedIn_ShouldThrow() {
        setupMethod("defaultMethod");
        SecurityContextHolder.clearContext();

        assertThrows(BusinessException.class, () -> permissionAspect.checkPermission(joinPoint));
    }

    @Test
    @DisplayName("权限校验 - 普通用户通过默认权限")
    void checkPermission_DefaultPermission_ShouldProceed() throws Throwable {
        setupMethod("defaultMethod");
        setUser(1, 1);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = permissionAspect.checkPermission(joinPoint);
        assertEquals("ok", result);
    }

    @Test
    @DisplayName("权限校验 - adminOnly非管理员抛出异常")
    void checkPermission_AdminOnly_NonAdmin_ShouldThrow() {
        setupMethod("adminOnlyMethod");
        setUser(1, 1); // enterprise user

        assertThrows(BusinessException.class, () -> permissionAspect.checkPermission(joinPoint));
    }

    @Test
    @DisplayName("权限校验 - adminOnly管理员通过")
    void checkPermission_AdminOnly_Admin_ShouldProceed() throws Throwable {
        setupMethod("adminOnlyMethod");
        setUser(1, 99); // admin
        when(joinPoint.proceed()).thenReturn("admin_ok");

        Object result = permissionAspect.checkPermission(joinPoint);
        assertEquals("admin_ok", result);
    }

    @Test
    @DisplayName("权限校验 - enterpriseOnly非企业用户抛出异常")
    void checkPermission_EnterpriseOnly_NonEnterprise_ShouldThrow() {
        setupMethod("enterpriseOnlyMethod");
        setUser(2, 2); // reviewer

        assertThrows(BusinessException.class, () -> permissionAspect.checkPermission(joinPoint));
    }

    @Test
    @DisplayName("权限校验 - reviewerOnly非审核员抛出异常")
    void checkPermission_ReviewerOnly_NonReviewer_ShouldThrow() {
        setupMethod("reviewerOnlyMethod");
        setUser(1, 1); // enterprise

        assertThrows(BusinessException.class, () -> permissionAspect.checkPermission(joinPoint));
    }

    @Test
    @DisplayName("权限校验 - thirdPartyOnly非第三方抛出异常")
    void checkPermission_ThirdPartyOnly_NonThirdParty_ShouldThrow() {
        setupMethod("thirdPartyOnlyMethod");
        setUser(1, 1); // enterprise

        assertThrows(BusinessException.class, () -> permissionAspect.checkPermission(joinPoint));
    }

    @Test
    @DisplayName("权限校验 - API路径权限配置拒绝时抛出异常")
    void checkPermission_ApiDenied_ShouldThrow() {
        setupMethod("defaultMethod");
        setUser(1, 1);
        setHttpRequest("POST", "/api/v1/admin/users");

        EntryPermission perm = EntryPermission.builder().userType(1).apiPath("/api/v1/admin/users")
                .httpMethod("POST").allowed(false).build();
        when(entryPermissionRepository.findByApiPathAndHttpMethodAndDeletedFalse(anyString(), anyString()))
                .thenReturn(Optional.of(perm));

        assertThrows(BusinessException.class, () -> permissionAspect.checkPermission(joinPoint));
    }

    @Test
    @DisplayName("权限校验 - API路径用户类型不匹配时抛出异常")
    void checkPermission_UserTypeMismatch_ShouldThrow() {
        setupMethod("defaultMethod");
        setUser(1, 1); // enterprise
        setHttpRequest("GET", "/api/v1/reviewer/data");

        EntryPermission perm = EntryPermission.builder().userType(2).apiPath("/api/v1/reviewer/data")
                .httpMethod("GET").allowed(true).build();
        when(entryPermissionRepository.findByApiPathAndHttpMethodAndDeletedFalse(anyString(), anyString()))
                .thenReturn(Optional.of(perm));

        assertThrows(BusinessException.class, () -> permissionAspect.checkPermission(joinPoint));
    }

    @Test
    @DisplayName("权限校验 - 管理员跳过用户类型检查")
    void checkPermission_AdminSkipsTypeCheck() throws Throwable {
        setupMethod("defaultMethod");
        setUser(1, 99); // admin
        setHttpRequest("GET", "/api/v1/reviewer/data");

        EntryPermission perm = EntryPermission.builder().userType(2).apiPath("/api/v1/reviewer/data")
                .httpMethod("GET").allowed(true).build();
        when(entryPermissionRepository.findByApiPathAndHttpMethodAndDeletedFalse(anyString(), anyString()))
                .thenReturn(Optional.of(perm));
        when(joinPoint.proceed()).thenReturn("admin_bypass");

        Object result = permissionAspect.checkPermission(joinPoint);
        assertEquals("admin_bypass", result);
    }

    @Test
    @DisplayName("权限校验 - 无HTTP请求时正常通过")
    void checkPermission_NoHttpRequest_ShouldProceed() throws Throwable {
        setupMethod("defaultMethod");
        setUser(1, 1);
        RequestContextHolder.resetRequestAttributes();
        when(joinPoint.proceed()).thenReturn("no_request");

        Object result = permissionAspect.checkPermission(joinPoint);
        assertEquals("no_request", result);
    }

    @Test
    @DisplayName("权限校验 - 带权限编码的注解正常通过")
    void checkPermission_WithPermissionCode_ShouldProceed() throws Throwable {
        setupMethod("withPermissionCodeMethod");
        setUser(1, 1);
        when(accountPermissionListRepository.findByPermissionCodeAndDeletedFalse("carbon:report:submit"))
                .thenReturn(Optional.of(AccountPermissionList.builder().build()));
        when(joinPoint.proceed()).thenReturn("code_ok");

        Object result = permissionAspect.checkPermission(joinPoint);
        assertEquals("code_ok", result);
    }

    @Test
    @DisplayName("权限校验 - userType为0或null时允许所有用户类型")
    void checkPermission_NullUserType_ShouldAllow() throws Throwable {
        setupMethod("defaultMethod");
        setUser(1, 1);
        setHttpRequest("GET", "/api/v1/public/data");

        EntryPermission perm = EntryPermission.builder().userType(null).apiPath("/api/v1/public/data")
                .httpMethod("GET").allowed(true).build();
        when(entryPermissionRepository.findByApiPathAndHttpMethodAndDeletedFalse(anyString(), anyString()))
                .thenReturn(Optional.of(perm));
        when(joinPoint.proceed()).thenReturn("public_ok");

        Object result = permissionAspect.checkPermission(joinPoint);
        assertEquals("public_ok", result);
    }

    private void setupMethod(String methodName) {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        try {
            Method method = TestMethods.class.getMethod(methodName);
            when(methodSignature.getMethod()).thenReturn(method);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void setUser(long userId, int userType) {
        String role = switch (userType) {
            case 1 -> "ENTERPRISE";
            case 2 -> "REVIEWER";
            case 3 -> "THIRD_PARTY";
            case 99 -> "ADMIN";
            default -> "USER";
        };
        JwtUserDetails user = JwtUserDetails.builder()
                .userId(userId).username("user" + userId).userType(userType)
                .roles(List.of(role)).enabled(true).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private void setHttpRequest(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setContextPath("");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    /** Test method container */
    @SuppressWarnings("unused")
    public static class TestMethods {
        @RequirePermission
        public void defaultMethod() {}

        @RequirePermission(adminOnly = true)
        public void adminOnlyMethod() {}

        @RequirePermission(enterpriseOnly = true)
        public void enterpriseOnlyMethod() {}

        @RequirePermission(reviewerOnly = true)
        public void reviewerOnlyMethod() {}

        @RequirePermission(thirdPartyOnly = true)
        public void thirdPartyOnlyMethod() {}

        @RequirePermission("carbon:report:submit")
        public void withPermissionCodeMethod() {}
    }
}

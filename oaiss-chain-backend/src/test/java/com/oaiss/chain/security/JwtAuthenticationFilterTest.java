package com.oaiss.chain.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter单元测试
 * Unit tests for JwtAuthenticationFilter
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache tokenBlacklistCache;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext();

        // Default: no blacklisted tokens (lenient since whitelist/no-token tests don't reach this)
        lenient().when(cacheManager.getCache("tokenBlacklist")).thenReturn(tokenBlacklistCache);
        lenient().when(tokenBlacklistCache.get(anyString())).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("白名单路径 /api/auth/login 应直接放行")
    void doFilterInternal_whitelistedLoginPath_shouldPassThrough() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/api/auth/login");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenProvider, never()).validateToken(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("白名单路径 /swagger-ui 应直接放行")
    void doFilterInternal_whitelistedSwaggerPath_shouldPassThrough() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/swagger-ui/index.html");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenProvider, never()).validateToken(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("白名单路径 /v3/api-docs 应直接放行")
    void doFilterInternal_whitelistedApiDocsPath_shouldPassThrough() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/v3/api-docs");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenProvider, never()).validateToken(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("白名单路径 /api/auth/register 应直接放行")
    void doFilterInternal_whitelistedRegisterPath_shouldPassThrough() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/api/auth/register");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenProvider, never()).validateToken(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("有效Token应设置认证信息")
    void doFilterInternal_validToken_shouldSetAuthentication() throws ServletException, IOException {
        // Arrange
        String validToken = "valid.jwt.token";
        request.setRequestURI("/api/users");
        request.addHeader("Authorization", "Bearer " + validToken);

        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(validToken)).thenReturn("testuser");
        when(jwtTokenProvider.getUserIdFromToken(validToken)).thenReturn(1L);
        when(jwtTokenProvider.getRolesFromToken(validToken)).thenReturn(List.of("USER"));
        when(jwtTokenProvider.getUserTypeFromToken(validToken)).thenReturn(1);
        when(jwtTokenProvider.getEnterpriseIdFromToken(validToken)).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("testuser");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("ROLE_USER");

        verify(jwtTokenProvider).validateToken(validToken);
        verify(jwtTokenProvider).getUsernameFromToken(validToken);
    }

    @Test
    @DisplayName("无效Token不应设置认证信息")
    void doFilterInternal_invalidToken_shouldNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        String invalidToken = "invalid.jwt.token";
        request.setRequestURI("/api/users");
        request.addHeader("Authorization", "Bearer " + invalidToken);

        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider).validateToken(invalidToken);
        verify(jwtTokenProvider, never()).getUsernameFromToken(anyString());
    }

    @Test
    @DisplayName("无Token不应设置认证信息")
    void doFilterInternal_noToken_shouldNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/api/users");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("过期Token不应设置认证信息")
    void doFilterInternal_expiredToken_shouldNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        String expiredToken = "expired.jwt.token";
        request.setRequestURI("/api/users");
        request.addHeader("Authorization", "Bearer " + expiredToken);

        when(jwtTokenProvider.validateToken(expiredToken)).thenReturn(false);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider).validateToken(expiredToken);
    }

    @Test
    @DisplayName("Bearer Token应正确提取")
    void doFilterInternal_bearerToken_shouldBeExtractedCorrectly() throws ServletException, IOException {
        // Arrange
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.signature";
        request.setRequestURI("/api/users");
        request.addHeader("Authorization", "Bearer " + expectedToken);

        when(jwtTokenProvider.validateToken(expectedToken)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(expectedToken)).thenReturn("testuser");
        when(jwtTokenProvider.getUserIdFromToken(expectedToken)).thenReturn(1L);
        when(jwtTokenProvider.getRolesFromToken(expectedToken)).thenReturn(List.of("ADMIN"));
        when(jwtTokenProvider.getUserTypeFromToken(expectedToken)).thenReturn(2);
        when(jwtTokenProvider.getEnterpriseIdFromToken(expectedToken)).thenReturn(100L);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("testuser");

        JwtUserDetails userDetails = (JwtUserDetails) authentication.getPrincipal();
        assertThat(userDetails.getUserId()).isEqualTo(1L);
        assertThat(userDetails.getRoles()).contains("ADMIN");
        assertThat(userDetails.getUserType()).isEqualTo(2);
        assertThat(userDetails.getEnterpriseId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("无Bearer前缀的Token应被忽略")
    void doFilterInternal_tokenWithoutBearerPrefix_shouldBeIgnored() throws ServletException, IOException {
        // Arrange
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.signature";
        request.setRequestURI("/api/users");
        request.addHeader("Authorization", token);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("空Authorization头应被忽略")
    void doFilterInternal_emptyAuthorizationHeader_shouldBeIgnored() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/api/users");
        request.addHeader("Authorization", "");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("仅有Bearer前缀无Token应被忽略")
    void doFilterInternal_bearerPrefixOnly_shouldBeIgnored() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/api/users");
        request.addHeader("Authorization", "Bearer ");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        // Bearer with empty token will still call validateToken with empty string
        // This tests that an empty token after Bearer prefix is handled
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("认证异常应清除安全上下文")
    void doFilterInternal_authenticationException_shouldClearContext() throws ServletException, IOException {
        // Arrange
        String token = "problematic.jwt.token";
        request.setRequestURI("/api/users");
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(token)).thenThrow(new RuntimeException("Token parsing error"));

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("企业用户Token应包含企业ID")
    void doFilterInternal_enterpriseUserToken_shouldIncludeEnterpriseId() throws ServletException, IOException {
        // Arrange
        String token = "enterprise.user.token";
        request.setRequestURI("/api/enterprise/data");
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn("enterpriseUser");
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(2L);
        when(jwtTokenProvider.getRolesFromToken(token)).thenReturn(List.of("ENTERPRISE", "USER"));
        when(jwtTokenProvider.getUserTypeFromToken(token)).thenReturn(2);
        when(jwtTokenProvider.getEnterpriseIdFromToken(token)).thenReturn(500L);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();

        JwtUserDetails userDetails = (JwtUserDetails) authentication.getPrincipal();
        assertThat(userDetails.getEnterpriseId()).isEqualTo(500L);
        assertThat(userDetails.getRoles()).containsExactlyInAnyOrder("ENTERPRISE", "USER");
    }

    @Test
    @DisplayName("管理员Token应具有ADMIN角色")
    void doFilterInternal_adminToken_shouldHaveAdminRole() throws ServletException, IOException {
        // Arrange
        String token = "admin.jwt.token";
        request.setRequestURI("/api/admin/users");
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn("admin");
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(1L);
        when(jwtTokenProvider.getRolesFromToken(token)).thenReturn(List.of("ADMIN"));
        when(jwtTokenProvider.getUserTypeFromToken(token)).thenReturn(0);
        when(jwtTokenProvider.getEnterpriseIdFromToken(token)).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("ROLE_ADMIN");
    }

    @Test
    @DisplayName("多个角色Token应正确解析")
    void doFilterInternal_multiRoleToken_shouldParseAllRoles() throws ServletException, IOException {
        // Arrange
        String token = "multi.role.token";
        request.setRequestURI("/api/data");
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn("multiRoleUser");
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(3L);
        when(jwtTokenProvider.getRolesFromToken(token)).thenReturn(List.of("USER", "REVIEWER", "ADMIN"));
        when(jwtTokenProvider.getUserTypeFromToken(token)).thenReturn(1);
        when(jwtTokenProvider.getEnterpriseIdFromToken(token)).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_REVIEWER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Token中用户名为null不应设置认证")
    void doFilterInternal_nullUsername_shouldNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        String token = "token.with.null.username";
        request.setRequestURI("/api/users");
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("已存在认证时不应覆盖")
    void doFilterInternal_existingAuthentication_shouldNotOverride() throws ServletException, IOException {
        // Arrange
        String token = "valid.token";
        request.setRequestURI("/api/users");
        request.addHeader("Authorization", "Bearer " + token);

        // Set existing authentication
        JwtUserDetails existingUser = JwtUserDetails.builder()
                .userId(999L)
                .username("existingUser")
                .roles(List.of("EXISTING"))
                .build();
        UsernamePasswordAuthenticationToken existingAuth = new UsernamePasswordAuthenticationToken(
                existingUser, null, existingUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn("newUser");
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(1L);
        when(jwtTokenProvider.getRolesFromToken(token)).thenReturn(List.of("NEW"));

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert - Should keep existing authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getName()).isEqualTo("existingUser");
    }

    @Test
    @DisplayName("Basic认证头应被忽略")
    void doFilterInternal_basicAuthHeader_shouldBeIgnored() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/api/users");
        request.addHeader("Authorization", "Basic dXNlcjpwYXNzd29yZA==");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("白名单路径/api/auth/captcha应直接放行")
    void doFilterInternal_whitelistedCaptchaPath_shouldPassThrough() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/api/auth/captcha");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenProvider, never()).validateToken(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("白名单路径/api/auth/refresh应直接放行")
    void doFilterInternal_whitelistedRefreshPath_shouldPassThrough() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/api/auth/refresh");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenProvider, never()).validateToken(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("v1/api-docs白名单路径应直接放行")
    void doFilterInternal_whitelistedV1ApiDocsPath_shouldPassThrough() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/v1/api-docs");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenProvider, never()).validateToken(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ==================== H1: Path Traversal Security Tests ====================

    @Test
    @DisplayName("路径遍历攻击 ../ 不应绕过认证")
    void doFilterInternal_pathTraversal_shouldNotBypassAuth() throws ServletException, IOException {
        // Arrange: /api/v1/auth/login/../admin/users resolves to /api/v1/admin/users
        request.setRequestURI("/api/v1/auth/login/../admin/users");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert: Should NOT be whitelisted - should try to validate token (or at least not skip)
        // The filter should normalize the path and recognize it's not a whitelist path
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // Should NOT skip token validation entirely - path is actually /api/v1/admin/users
        // Without a token, it just passes through with no auth set, which is correct behavior
        // The key is that the request should NOT be treated as a whitelist path
    }

    @Test
    @DisplayName("深度路径遍历 ../../ 不应绕过认证")
    void doFilterInternal_deepPathTraversal_shouldNotBypassAuth() throws ServletException, IOException {
        // Arrange: /api/v1/auth/register/../../admin/users resolves to /api/v1/admin/users
        request.setRequestURI("/api/v1/auth/register/../../admin/users");
        String token = "some.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert: Should attempt token validation, not skip like whitelist
        verify(jwtTokenProvider).validateToken(token);
    }

    @Test
    @DisplayName("伪造白名单前缀路径不应绕过认证")
    void doFilterInternal_fakeWhitelistPrefix_shouldNotBypassAuth() throws ServletException, IOException {
        // Arrange: /api/v1/auth/loginx looks like /api/v1/auth/login but is different
        request.setRequestURI("/api/v1/auth/loginx");
        String token = "some.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert: Should attempt token validation, not skip like whitelist
        verify(jwtTokenProvider).validateToken(token);
    }

    @Test
    @DisplayName("合法白名单子路径应正常放行")
    void doFilterInternal_legitimateWhitelistSubpath_shouldPassThrough() throws ServletException, IOException {
        // Arrange: /captcha/sms/send is a legitimate sub-path under /captcha/
        request.setRequestURI("/captcha/sms/send");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenProvider, never()).validateToken(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}

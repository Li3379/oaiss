package com.oaiss.chain.service;

import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.dto.ApiResponse;
import com.oaiss.chain.dto.LoginRequest;
import com.oaiss.chain.dto.LoginResponse;
import com.oaiss.chain.dto.RegisterRequest;
import com.oaiss.chain.entity.Enterprise;
import com.oaiss.chain.entity.User;
import com.oaiss.chain.enums.UserTypeEnum;
import com.oaiss.chain.exception.AuthenticationException;
import com.oaiss.chain.exception.BusinessException;
import com.oaiss.chain.repository.EnterpriseRepository;
import com.oaiss.chain.repository.UserRepository;
import com.oaiss.chain.security.JwtTokenProvider;
import com.oaiss.chain.security.JwtUserDetails;
import com.oaiss.chain.service.CaptchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试
 * AuthService Unit Tests
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnterpriseRepository enterpriseRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private MetricsService metricsService;
    @Mock
    private CaptchaService captchaService;


    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .password("encodedPassword")
                .userType(UserTypeEnum.ENTERPRISE.getCode())
                .status(1)
                .realName("Test User")
                .build();

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("password123");
        registerRequest.setConfirmPassword("password123");
        registerRequest.setUserType(UserTypeEnum.ENTERPRISE.getCode());
    }

    @Test
    @DisplayName("登录成功测试")
    void testLoginSuccess() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), any(), anyList(), any(), any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(), any())).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(enterpriseRepository.findByUserId(any())).thenReturn(Optional.empty());

        // When
        LoginResponse response = authService.login(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        verify(metricsService, times(1)).incrementUserLogin(any(), eq(true));
        verify(metricsService, times(1)).incrementActiveUsers();
    }

    @Test
    @DisplayName("登录失败-用户不存在")
    void testLoginFailUserNotFound() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AuthenticationException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("登录失败-密码错误")
    void testLoginFailWrongPassword() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        // When & Then
        assertThrows(AuthenticationException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("登录失败-账号禁用")
    void testLoginFailAccountDisabled() {
        // Given
        testUser.setStatus(0);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        // When & Then
        assertThrows(AuthenticationException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("注册成功测试")
    void testRegisterSuccess() {
        // Given
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user;
        });
        when(jwtTokenProvider.generateAccessToken(any(), any(), anyList(), any(), any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(), any())).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);

        // When
        LoginResponse response = authService.register(registerRequest);

        // Then
        assertNotNull(response);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("注册失败-用户名已存在")
    void testRegisterFailUsernameExists() {
        // Given
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        // When & Then
        assertThrows(BusinessException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("注册失败-密码不一致")
    void testRegisterFailPasswordMismatch() {
        // Given
        registerRequest.setConfirmPassword("differentPassword");

        // When & Then
        assertThrows(AuthenticationException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("生成验证码成功")
    void testGenerateCaptcha() {
        // Given: CaptchaService returns a valid response
        com.oaiss.chain.dto.CaptchaResponse captchaServiceResponse =
                com.oaiss.chain.dto.CaptchaResponse.builder()
                        .captchaKey("CAP_12345")
                        .captchaImage("data:image/png;base64,iVBORw0KGgo=")
                        .expiresIn(300L)
                        .build();
        when(captchaService.generateCaptcha()).thenReturn(captchaServiceResponse);

        // When
        var response = authService.generateCaptcha();

        // Then
        assertNotNull(response);
        assertNotNull(response.getCaptchaKey());
        assertNotNull(response.getCaptchaImage());
    }

    // ========== TDD Cycle 4: C1 登出未实际失效Token ==========

    @Test
    @DisplayName("登出成功-Token应被加入黑名单")
    void testLogoutShouldBlacklistToken() {
        // Given
        Cache mockCache = mock(Cache.class);
        when(cacheManager.getCache("tokenBlacklist")).thenReturn(mockCache);

        // When
        authService.logout("testuser", "some-jwt-token");

        // Then - Token should be put into the blacklist cache
        verify(cacheManager).getCache("tokenBlacklist");
        verify(mockCache).put(eq("some-jwt-token"), any());
    }

    @Test
    @DisplayName("刷新Token成功")
    void testRefreshTokenSuccess() {
        // Given
        when(jwtTokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("refresh-token")).thenReturn("testuser");
        when(jwtTokenProvider.getUserIdFromToken("refresh-token")).thenReturn(1L);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(any(), any(), anyList(), any(), any())).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(any(), any())).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);
        when(enterpriseRepository.findByUserId(any())).thenReturn(Optional.empty());

        // When
        LoginResponse response = authService.refreshToken("refresh-token");

        // Then
        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
    }

    @Test
    @DisplayName("刷新Token失败-Token无效")
    void testRefreshTokenFailInvalidToken() {
        // Given
        when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

        // When & Then
        assertThrows(AuthenticationException.class, () -> authService.refreshToken("invalid-token"));
    }

    @Test
    @DisplayName("刷新Token失败-非RefreshToken")
    void testRefreshTokenFailNotRefreshToken() {
        // Given
        when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("access-token")).thenReturn(false);

        // When & Then
        assertThrows(AuthenticationException.class, () -> authService.refreshToken("access-token"));
    }

    @Test
    @DisplayName("刷新Token失败-用户不存在")
    void testRefreshTokenFailUserNotFound() {
        // Given
        when(jwtTokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("refresh-token")).thenReturn("nonexistent");
        when(jwtTokenProvider.getUserIdFromToken("refresh-token")).thenReturn(999L);
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AuthenticationException.class, () -> authService.refreshToken("refresh-token"));
    }

    @Test
    @DisplayName("刷新Token失败-账号禁用")
    void testRefreshTokenFailAccountDisabled() {
        // Given
        testUser.setStatus(0);
        when(jwtTokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("refresh-token")).thenReturn("testuser");
        when(jwtTokenProvider.getUserIdFromToken("refresh-token")).thenReturn(1L);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(AuthenticationException.class, () -> authService.refreshToken("refresh-token"));
    }

    @Test
    @DisplayName("刷新Token成功-企业用户带企业ID")
    void testRefreshTokenSuccessWithEnterpriseId() {
        // Given
        Enterprise enterprise = new Enterprise();
        enterprise.setId(100L);
        testUser.setId(1L);
        testUser.setUserType(1); // Enterprise user
        when(jwtTokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("refresh-token")).thenReturn("testuser");
        when(jwtTokenProvider.getUserIdFromToken("refresh-token")).thenReturn(1L);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(enterprise));
        when(jwtTokenProvider.generateAccessToken(any(), any(), anyList(), any(), any())).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(any(), any())).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);

        // When
        LoginResponse response = authService.refreshToken("refresh-token");

        // Then
        assertNotNull(response);
    }

    @Test
    @DisplayName("修改密码成功")
    void testChangePasswordSuccess() {
        // Given
        com.oaiss.chain.dto.PasswordChangeRequest request = new com.oaiss.chain.dto.PasswordChangeRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        authService.changePassword("testuser", request);

        // Then
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("修改密码失败-用户不存在")
    void testChangePasswordFailUserNotFound() {
        // Given
        com.oaiss.chain.dto.PasswordChangeRequest request = new com.oaiss.chain.dto.PasswordChangeRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");
        
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(BusinessException.class, () -> authService.changePassword("nonexistent", request));
    }

    @Test
    @DisplayName("修改密码失败-原密码错误")
    void testChangePasswordFailWrongOldPassword() {
        // Given
        com.oaiss.chain.dto.PasswordChangeRequest request = new com.oaiss.chain.dto.PasswordChangeRequest();
        request.setOldPassword("wrongOldPassword");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongOldPassword", "encodedPassword")).thenReturn(false);

        // When & Then
        assertThrows(BusinessException.class, () -> authService.changePassword("testuser", request));
    }

    @Test
    @DisplayName("修改密码失败-新密码不一致")
    void testChangePasswordFailPasswordMismatch() {
        // Given
        com.oaiss.chain.dto.PasswordChangeRequest request = new com.oaiss.chain.dto.PasswordChangeRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("differentPassword");
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);

        // When & Then
        assertThrows(BusinessException.class, () -> authService.changePassword("testuser", request));
    }

    @Test
    @DisplayName("检查IP成功-用户存在")
    void testCheckIpSuccess() {
        // Given
        testUser.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        var response = authService.checkIp(1L);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getCode());
    }

    @Test
    @DisplayName("检查IP失败-用户不存在")
    void testCheckIpFailUserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(BusinessException.class, () -> authService.checkIp(999L));
    }

    @Test
    @DisplayName("获取当前用户-有认证信息")
    void testGetCurrentUserWithAuth() {
        // Given
        JwtUserDetails userDetails = JwtUserDetails.builder()
                .userId(1L)
                .username("testuser")
                .build();
        org.springframework.security.core.Authentication auth = 
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(userDetails, null);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        // When
        JwtUserDetails result = authService.getCurrentUser();

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        
        // Cleanup
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("获取当前用户-无认证信息")
    void testGetCurrentUserWithoutAuth() {
        // Given
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        // When
        JwtUserDetails result = authService.getCurrentUser();

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("登录成功-带验证码")
    void testLoginSuccessWithCaptcha() {
        // Given
        loginRequest.setCaptchaKey("captcha_key");
        loginRequest.setCaptcha("1234");
        when(captchaService.verifyCaptcha("captcha_key", "1234")).thenReturn(true);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), any(), anyList(), any(), any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(), any())).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(enterpriseRepository.findByUserId(any())).thenReturn(Optional.empty());

        // When
        LoginResponse response = authService.login(loginRequest);

        // Then
    }

    @Test
    @DisplayName("登录失败-验证码错误")
    void testLoginFailCaptchaError() {
        // Given
        loginRequest.setCaptchaKey("captcha_key");
        loginRequest.setCaptcha("wrong");
        
        when(captchaService.verifyCaptcha("captcha_key", "wrong")).thenReturn(false);

        // When & Then
        assertThrows(AuthenticationException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("登录失败-验证码过期")
    void testLoginFailCaptchaExpired() {
        // Given
        loginRequest.setCaptchaKey("captcha_key");
        when(captchaService.verifyCaptcha("captcha_key", "1234")).thenReturn(false);
        loginRequest.setCaptcha("1234");

        // When & Then
        assertThrows(AuthenticationException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("登录成功-企业用户带企业ID")
    void testLoginSuccessWithEnterpriseId() {
        // Given
        Enterprise enterprise = new Enterprise();
        enterprise.setId(100L);
        testUser.setId(1L);
        testUser.setUserType(1); // Enterprise user
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), any(), anyList(), any(), any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(), any())).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(enterprise));

        // When
        LoginResponse response = authService.login(loginRequest);

        // Then
        assertNotNull(response);
    }

    @Test
    @DisplayName("注册成功-带手机号")
    void testRegisterSuccessWithPhone() {
        // Given
        registerRequest.setPhone("13800138000");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByPhone("13800138000")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user;
        });
        when(jwtTokenProvider.generateAccessToken(any(), any(), anyList(), any(), any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(), any())).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);

        // When
        LoginResponse response = authService.register(registerRequest);

        // Then
        assertNotNull(response);
    }

    @Test
    @DisplayName("注册失败-手机号已存在")
    void testRegisterFailPhoneExists() {
        // Given
        registerRequest.setPhone("13800138000");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByPhone("13800138000")).thenReturn(true);

        // When & Then
        assertThrows(BusinessException.class, () -> authService.register(registerRequest));
    }

    @Test
    @DisplayName("生成验证码-始终通过CaptchaService返回PNG数据URI")
    void testGenerateCaptchaCacheNull() {
        // Given: CaptchaService handles storage internally, no CacheManager dependency
        com.oaiss.chain.dto.CaptchaResponse captchaServiceResponse =
                com.oaiss.chain.dto.CaptchaResponse.builder()
                        .captchaKey("CAP_67890")
                        .captchaImage("data:image/png;base64,iVBORw0KGgo=")
                        .expiresIn(300L)
                        .build();
        when(captchaService.generateCaptcha()).thenReturn(captchaServiceResponse);

        // When
        var response = authService.generateCaptcha();

        // Then
        assertNotNull(response);
        assertNotNull(response.getCaptchaKey());
        assertTrue(response.getCaptchaImage().startsWith("data:image/png;base64,"));
    }

    // ==================== H16: Registration User Type Restriction ====================

    @Test
    @DisplayName("注册失败-不允许注册管理员类型(4)")
    void testRegisterFailAdminTypeNotAllowed() {
        // Given
        registerRequest.setUserType(UserTypeEnum.ADMIN.getCode()); // 4

        // When & Then
        assertThrows(BusinessException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("注册失败-不允许注册审核员类型(2)")
    void testRegisterFailReviewerTypeNotAllowed() {
        // Given
        registerRequest.setUserType(UserTypeEnum.REVIEWER.getCode()); // 2

        // When & Then
        assertThrows(BusinessException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("注册成功-企业用户类型(1)应允许")
    void testRegisterSuccessEnterpriseTypeAllowed() {
        // Given
        registerRequest.setUserType(UserTypeEnum.ENTERPRISE.getCode()); // 1
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(any(), any(), anyList(), any(), any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(), any())).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);

        // When & Then
        assertDoesNotThrow(() -> authService.register(registerRequest));
    }

    @Test
    @DisplayName("注册成功-第三方监管类型(3)应允许")
    void testRegisterSuccessThirdPartyTypeAllowed() {
        // Given
        registerRequest.setUserType(UserTypeEnum.THIRD_PARTY.getCode()); // 3
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(any(), any(), anyList(), any(), any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(), any())).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);

        // When & Then
        assertDoesNotThrow(() -> authService.register(registerRequest));
    }

    // ==================== H4: Login Rate Limiting ====================

    @Test
    @DisplayName("连续失败5次后第6次登录应被频率限制")
    void testLoginRateLimitedAfterFiveFailures() {
        // Given: user has 5 failed attempts cached
        Cache mockAttemptsCache = mock(Cache.class);
        when(cacheManager.getCache("loginAttempts")).thenReturn(mockAttemptsCache);

        // Simulate 5 failed attempts already recorded
        when(mockAttemptsCache.get("login_fail_testuser")).thenReturn(
                new Cache.ValueWrapper() {
                    @Override public Object get() { return 5; }
                });

        // When & Then: 6th attempt should be rate limited
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> authService.login(loginRequest));
        // Verify password was NOT checked (blocked before reaching it)
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("失败次数未达上限时应正常允许登录尝试")
    void testLoginNotRateLimitedUnderThreshold() {
        // Given: user has 3 failed attempts (under threshold of 5)
        Cache mockAttemptsCache = mock(Cache.class);
        when(cacheManager.getCache("loginAttempts")).thenReturn(mockAttemptsCache);
        when(mockAttemptsCache.get("login_fail_testuser")).thenReturn(
                new Cache.ValueWrapper() {
                    @Override public Object get() { return 3; }
                });
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        // When & Then: Should proceed normally (throw login failed, not rate limit)
        assertThrows(AuthenticationException.class, () -> authService.login(loginRequest));
        // Verify password WAS checked (not blocked)
        verify(passwordEncoder).matches("password123", "encodedPassword");
    }

    @Test
    @DisplayName("登录成功后应清除失败计数")
    void testLoginSuccessClearsFailedAttempts() {
        // Given: user has 3 failed attempts
        Cache mockAttemptsCache = mock(Cache.class);
        when(cacheManager.getCache("loginAttempts")).thenReturn(mockAttemptsCache);
        when(mockAttemptsCache.get("login_fail_testuser")).thenReturn(
                new Cache.ValueWrapper() {
                    @Override public Object get() { return 3; }
                });
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), any(), anyList(), any(), any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(), any())).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(enterpriseRepository.findByUserId(any())).thenReturn(Optional.empty());

        // When
        LoginResponse response = authService.login(loginRequest);

        // Then
        assertNotNull(response);
        verify(mockAttemptsCache).evict("login_fail_testuser");
    }

    @Test
    @DisplayName("无失败记录时应正常允许登录")
    void testLoginNoRateLimitWhenNoFailures() {
        // Given: no failed attempts cached
        Cache mockAttemptsCache = mock(Cache.class);
        when(cacheManager.getCache("loginAttempts")).thenReturn(mockAttemptsCache);
        when(mockAttemptsCache.get("login_fail_testuser")).thenReturn(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AuthenticationException.class, () -> authService.login(loginRequest));
    }

    // ==================== C8: Real Captcha Image Generation ====================

    @Test
    @DisplayName("生成验证码应委托给CaptchaService并返回PNG数据URI")
    void testGenerateCaptchaDelegatesToCaptchaService() {
        // Given: CaptchaService returns a proper PNG data URI
        com.oaiss.chain.dto.CaptchaResponse captchaServiceResponse =
                com.oaiss.chain.dto.CaptchaResponse.builder()
                        .captchaKey("CAP_12345")
                        .captchaImage("data:image/png;base64,iVBORw0KGgo=")
                        .expiresIn(300L)
                        .build();
        when(captchaService.generateCaptcha()).thenReturn(captchaServiceResponse);

        // When
        var response = authService.generateCaptcha();

        // Then: should delegate to CaptchaService
        assertNotNull(response);
        assertEquals("CAP_12345", response.getCaptchaKey());
        assertTrue(response.getCaptchaImage().startsWith("data:image/png;base64,"),
                "Captcha image must be a PNG data URI, got: " + response.getCaptchaImage());
        verify(captchaService).generateCaptcha();
    }
}

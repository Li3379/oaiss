package com.oaiss.chain.service;

import com.oaiss.chain.dto.*;
import com.oaiss.chain.entity.Enterprise;
import com.oaiss.chain.enums.UserTypeEnum;
import com.oaiss.chain.exception.AuthenticationException;
import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.constant.ErrorMessage;
import com.oaiss.chain.repository.EnterpriseRepository;
import com.oaiss.chain.security.JwtTokenProvider;
import com.oaiss.chain.security.JwtUserDetails;
import com.oaiss.chain.util.CommonUtils;
import com.oaiss.chain.entity.User;
import com.oaiss.chain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 认证服务
 * 
 * @author OAISS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CacheManager cacheManager;
    private final MetricsService metricsService;
    private final CaptchaService captchaService;

    /**
     * 用户登录
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 1. 验证验证码（如果提供了）
        if (request.getCaptchaKey() != null && request.getCaptcha() != null) {
            validateCaptcha(request.getCaptchaKey(), request.getCaptcha());
        }

        // 2. 检查登录频率限制
        checkLoginRateLimit(request.getUsername());

        // 3. 查找用户
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    recordFailedLogin(request.getUsername());
                    return AuthenticationException.loginFailed(ErrorMessage.LOGIN_FAILED);
                });

        // 4. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordFailedLogin(request.getUsername());
            throw AuthenticationException.loginFailed(ErrorMessage.LOGIN_FAILED);
        }

        // 5. 检查账号状态
        if (user.getStatus() == 0) {
            throw AuthenticationException.accountDisabled();
        }

        // 6. IP验证
        validateIp(user);

        // 7. 获取企业ID（如果是企业用户）
        Long enterpriseId = null;
        if (user.getUserType() == 1) { // 企业用户
            enterpriseId = enterpriseRepository.findByUserId(user.getId())
                    .map(Enterprise::getId)
                    .orElse(null);
        }

        // 8. 生成Token
        String roleName = getRoleName(user.getUserType());
        List<String> roles = List.of(roleName);
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getUsername(), roles, user.getUserType(), enterpriseId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getUsername());

        // 9. 更新最后登录信息
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(CommonUtils.getClientIp());
        userRepository.save(user);

        // 10. 清除登录失败计数
        clearFailedLoginAttempts(request.getUsername());

        log.info("User logged in: {} (ID: {}, Type: {}, EnterpriseId: {})",
                user.getUsername(), user.getId(), user.getUserType(), enterpriseId);

        // 11. 记录登录成功指标
        metricsService.incrementUserLogin(UserTypeEnum.fromCode(user.getUserType()).getDescription(), true);
        metricsService.incrementActiveUsers();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration() / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .userType(user.getUserType())
                .realName(user.getRealName())
                .build();
    }

    /**
     * 用户注册
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        // 1. 校验密码一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw AuthenticationException.loginFailed("两次密码不一致");
        }

        // 2. 验证用户类型（禁止注册管理员和审核员）
        validateRegistrationUserType(request.getUserType());

        // 3. 检查用户名唯一
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new com.oaiss.chain.exception.BusinessException(
                    ErrorCode.ACCOUNT_EXISTS, ErrorMessage.ACCOUNT_EXISTS);
        }

        // 4. 检查手机号唯一
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new com.oaiss.chain.exception.BusinessException(
                    ErrorCode.PHONE_EXISTS, ErrorMessage.PHONE_EXISTS);
        }

        // 5. 创建用户
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .email(request.getEmail())
                .realName(request.getRealName())
                .userType(request.getUserType())
                .status(1)
                .build();

        user = userRepository.save(user);

        // 6. 生成Token
        String roleName = getRoleName(user.getUserType());
        List<String> roles = List.of(roleName);
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getUsername(), roles, user.getUserType(), null);
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getUsername());

        log.info("User registered: {} (ID: {}, Type: {})", 
                user.getUsername(), user.getId(), user.getUserType());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration() / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .userType(user.getUserType())
                .realName(user.getRealName())
                .build();
    }

    /**
     * 刷新Token
     */
    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) 
                || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw AuthenticationException.tokenInvalid();
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(AuthenticationException::tokenInvalid);

        if (user.getStatus() == 0) {
            throw AuthenticationException.accountDisabled();
        }

        // 获取企业ID（如果是企业用户）
        Long enterpriseId = null;
        if (user.getUserType() == 1) { // 企业用户
            enterpriseId = enterpriseRepository.findByUserId(user.getId())
                    .map(Enterprise::getId)
                    .orElse(null);
        }

        String roleName = getRoleName(user.getUserType());
        List<String> roles = List.of(roleName);

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getUsername(), roles, user.getUserType(), enterpriseId);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getUsername());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration() / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .userType(user.getUserType())
                .realName(user.getRealName())
                .build();
    }

    /**
     * 生成验证码
     * 委托给CaptchaService生成真实图形验证码（含干扰线/点的PNG图片）
     */
    public CaptchaResponse generateCaptcha() {
        return captchaService.generateCaptcha();
    }

    /**
     * IP地址验证
     */
    public ApiResponse<Boolean> checkIp(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.oaiss.chain.exception.BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));

        String clientIp = CommonUtils.getClientIp();
        boolean allowed = isIpAllowed(user, clientIp);

        return ApiResponse.success(allowed);
    }

    /**
     * 获取当前登录用户信息
     */
    public JwtUserDetails getCurrentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtUserDetails) {
            return (JwtUserDetails) auth.getPrincipal();
        }
        return null;
    }

    /**
     * 用户登出
     */
    public void logout(String username, String token) {
        // 将Token加入黑名单（使用Redis缓存实现）
        if (token != null && !token.isEmpty()) {
            Cache blacklist = cacheManager.getCache("tokenBlacklist");
            if (blacklist != null) {
                // 存入黑名单，值为当前时间戳，TTL由Redis缓存配置管理
                blacklist.put(token, System.currentTimeMillis());
            }
        }
        log.info("User logged out: {} (token blacklisted: {})", username, token != null);
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(String username, PasswordChangeRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.oaiss.chain.exception.BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));

        // 验证原密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new com.oaiss.chain.exception.BusinessException(
                    ErrorCode.OLD_PASSWORD_ERROR, ErrorMessage.OLD_PASSWORD_ERROR);
        }

        // 验证新密码一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new com.oaiss.chain.exception.BusinessException(
                    ErrorCode.PARAM_ERROR, "两次密码不一致");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("User password changed: {}", username);
    }

    // ==================== 私有方法 ====================

    /**
     * 允许注册的用户类型（管理员和审核员只能通过后台创建）
     */
    private static final Set<Integer> ALLOWED_REGISTRATION_TYPES = Set.of(
            UserTypeEnum.ENTERPRISE.getCode(),      // 1 - 企业用户
            UserTypeEnum.THIRD_PARTY.getCode(),     // 3 - 第三方监管
            UserTypeEnum.AUTHENTICATOR.getCode()    // 5 - 认证机构
    );

    /**
     * 登录失败最大尝试次数
     */
    private static final int MAX_LOGIN_ATTEMPTS = 5;

    // ==================== 登录频率限制 ====================

    /**
     * 检查登录频率限制
     * 超过MAX_LOGIN_ATTEMPTS次失败后拒绝登录
     */
    private void checkLoginRateLimit(String username) {
        Cache attemptsCache = cacheManager.getCache("loginAttempts");
        if (attemptsCache == null) return;

        String cacheKey = "login_fail_" + username;
        Cache.ValueWrapper wrapper = attemptsCache.get(cacheKey);
        if (wrapper != null && wrapper.get() instanceof Integer) {
            int attempts = (Integer) wrapper.get();
            if (attempts >= MAX_LOGIN_ATTEMPTS) {
                log.warn("Login rate limited for user: {} ({} attempts)", username, attempts);
                throw AuthenticationException.loginFailed("登录尝试过于频繁，请稍后再试");
            }
        }
    }

    /**
     * 记录登录失败次数
     */
    private void recordFailedLogin(String username) {
        Cache attemptsCache = cacheManager.getCache("loginAttempts");
        if (attemptsCache == null) return;

        String cacheKey = "login_fail_" + username;
        Cache.ValueWrapper wrapper = attemptsCache.get(cacheKey);
        int attempts = (wrapper != null && wrapper.get() instanceof Integer)
                ? (Integer) wrapper.get() : 0;
        attemptsCache.put(cacheKey, attempts + 1);
    }

    /**
     * 清除登录失败计数（登录成功后调用）
     */
    private void clearFailedLoginAttempts(String username) {
        Cache attemptsCache = cacheManager.getCache("loginAttempts");
        if (attemptsCache != null) {
            attemptsCache.evict("login_fail_" + username);
        }
    }

    // ==================== 注册类型校验 ====================

    /**
     * 验证注册时的用户类型是否允许
     * 管理员(4)和审核员(2)只能通过后台管理创建，不允许自行注册
     */
    private void validateRegistrationUserType(Integer userType) {
        if (userType == null || !ALLOWED_REGISTRATION_TYPES.contains(userType)) {
            throw new com.oaiss.chain.exception.BusinessException(
                    ErrorCode.PARAM_ERROR, "不允许注册此类型账号");
        }
    }

    private void validateCaptcha(String captchaKey, String captcha) {
        if (!captchaService.verifyCaptcha(captchaKey, captcha)) {
            throw AuthenticationException.captchaExpired();
        }
    }

    private void validateIp(User user) {
        if (user.getAllowedIps() == null || user.getAllowedIps().isEmpty()) {
            return;
        }

        String clientIp = CommonUtils.getClientIp();
        if (!isIpAllowed(user, clientIp)) {
            log.warn("IP validation failed for user {}: allowed IPs = {}, client IP = {}",
                    user.getUsername(), user.getAllowedIps(), clientIp);
            throw AuthenticationException.ipValidationFailed();
        }
    }

    private static final ObjectMapper IP_MAPPER = new ObjectMapper();

    private boolean isIpAllowed(User user, String clientIp) {
        if (user.getAllowedIps() == null || user.getAllowedIps().isEmpty()) {
            return true;
        }

        try {
            String[] ipArray = IP_MAPPER.readValue(user.getAllowedIps(), String[].class);
            return Arrays.stream(ipArray)
                    .anyMatch(ip -> ip.equals(clientIp) || ip.equals("*"));
        } catch (JsonProcessingException e) {
            log.error("Failed to parse IP whitelist for user {}: {}", user.getId(), e.getMessage());
            return false;
        }
    }

    private String getRoleName(Integer userType) {
        return UserTypeEnum.fromCode(userType).name();
    }
}

package com.oaiss.chain.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT认证过滤器
 * JWT Authentication Filter
 * 
 * 拦截请求，验证JWT令牌，设置安全上下文
 * 
 * @author OAISS Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CacheManager cacheManager;

    /**
     * 白名单路径（不需要认证）
     * 注意：context-path为/api/v1，request.getRequestURI()返回完整路径
     * 所以白名单需要包含完整路径前缀
     */
    private static final List<String> WHITELIST_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/captcha",
            "/api/v1/auth/refresh",
            "/api/v1/auth/check-ip",
            "/api/v1/captcha/",
            "/swagger-ui",
            "/v1/api-docs",
            "/v3/api-docs",
            "/actuator/health"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // 白名单路径直接放行
        if (isWhitelisted(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 提取Token
        String token = extractToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 检查Token是否已被加入黑名单（用户已登出）
            Cache blacklist = cacheManager.getCache("tokenBlacklist");
            if (blacklist != null && blacklist.get(token) != null) {
                log.debug("Token is blacklisted, skipping authentication");
                filterChain.doFilter(request, response);
                return;
            }

            try {
                // 从Token中获取用户信息
                String username = jwtTokenProvider.getUsernameFromToken(token);
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                List<String> roles = jwtTokenProvider.getRolesFromToken(token);
                Integer userType = jwtTokenProvider.getUserTypeFromToken(token);
                Long enterpriseId = jwtTokenProvider.getEnterpriseIdFromToken(token);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 构建用户详情
                    UserDetails userDetails = JwtUserDetails.builder()
                            .userId(userId)
                            .username(username)
                            .roles(roles)
                            .userType(userType)
                            .enterpriseId(enterpriseId)
                            .enabled(true)
                            .accountNonExpired(true)
                            .accountNonLocked(true)
                            .credentialsNonExpired(true)
                            .build();

                    // 构建认证令牌
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    // 设置安全上下文
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("Set authentication for user: {} (ID: {})", username, userId);
                }
            } catch (Exception e) {
                log.error("Cannot set user authentication: {}", e.getMessage());
                SecurityContextHolder.clearContext();
                EnterpriseContextHolder.clear();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 检查路径是否在白名单中
     * 安全措施：
     * 1. 先规范化路径（解析 ../ 和 ./ 防止路径遍历）
     * 2. 精确路径使用 equals 匹配
     * 3. 前缀路径（以/结尾）使用 startsWith 匹配
     */
    private boolean isWhitelisted(String requestPath) {
        String normalizedPath = normalizePath(requestPath);
        return WHITELIST_PATHS.stream()
                .anyMatch(whitelist -> {
                    if (whitelist.endsWith("/")) {
                        // Prefix match for directory-style paths (e.g., "/captcha/")
                        return normalizedPath.startsWith(whitelist);
                    }
                    // Exact match or immediate sub-path for specific endpoints
                    return normalizedPath.equals(whitelist)
                            || normalizedPath.startsWith(whitelist + "/");
                });
    }

    /**
     * 规范化路径，解析 ../ 和 ./ 防止路径遍历攻击
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        // Resolve . and .. segments
        String[] segments = path.split("/");
        List<String> resolved = new java.util.ArrayList<>();
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                if (!resolved.isEmpty()) {
                    resolved.remove(resolved.size() - 1);
                }
            } else {
                resolved.add(segment);
            }
        }
        return "/" + resolved.stream().collect(Collectors.joining("/"));
    }
}

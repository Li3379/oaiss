package com.oaiss.chain.aop;

import com.oaiss.chain.annotation.RateLimit;
import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.exception.BusinessException;
import com.oaiss.chain.security.JwtUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流切面
 * 
 * <p>拦截标注了@RateLimit注解的方法，进行限流控制</p>
 * <p>基于Redis实现分布式限流</p>
 * 
 * @author OAISS Chain Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    private static final String LUA_INCR_EXPIRE = """
            local current = redis.call('incr', KEYS[1])
            if current == 1 then
                redis.call('expire', KEYS[1], ARGV[1])
            end
            return current
            """;

    @Around("@annotation(com.oaiss.chain.annotation.RateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit annotation = method.getAnnotation(RateLimit.class);

        // 构建限流Key
        String key = buildRateLimitKey(annotation, joinPoint);
        
        // 获取时间窗口和限制次数
        int period = annotation.period();
        int limit = annotation.limit();

        try {
            // 使用Lua脚本原子操作：递增+设置过期
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_INCR_EXPIRE, Long.class);
            Long currentCount = redisTemplate.execute(script,
                    Collections.singletonList(key), String.valueOf(period));

            // 检查是否超过限制
            if (currentCount != null && currentCount > limit) {
                log.warn("Rate limit exceeded for key: {}, count: {}, limit: {}", key, currentCount, limit);
                throw new BusinessException(ErrorCode.REQUEST_TOO_FREQUENT, annotation.message());
            }

            log.debug("Rate limit check passed for key: {}, count: {}/{}", key, currentCount, limit);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Rate limit check failed for key: {}", key, e);
            // 限流检查失败时不阻止请求（降级处理）
        }

        // 执行原方法
        return joinPoint.proceed();
    }

    /**
     * 构建限流Key
     */
    private String buildRateLimitKey(RateLimit annotation, ProceedingJoinPoint joinPoint) {
        StringBuilder keyBuilder = new StringBuilder(RATE_LIMIT_KEY_PREFIX);

        // 添加自定义Key
        String customKey = annotation.key();
        if (customKey != null && !customKey.isEmpty()) {
            keyBuilder.append(customKey).append(":");
        } else {
            // 使用类名和方法名作为Key
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            keyBuilder.append(className).append(":").append(methodName).append(":");
        }

        // 根据限流类型添加标识
        RateLimit.LimitType limitType = annotation.limitType();
        
        switch (limitType) {
            case IP -> {
                String ip = getClientIp();
                keyBuilder.append("ip:").append(ip);
            }
            case USER -> {
                String userId = getCurrentUserId();
                keyBuilder.append("user:").append(userId);
            }
            case IP_USER -> {
                String ip = getClientIp();
                String userId = getCurrentUserId();
                keyBuilder.append("ip:").append(ip).append(":user:").append(userId);
            }
            default -> keyBuilder.append("global");
        }

        return keyBuilder.toString();
    }

    /**
     * 获取当前HTTP请求
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }

    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof JwtUserDetails userDetails) {
                return String.valueOf(userDetails.getUserId());
            }
        }
        return "anonymous";
    }
}

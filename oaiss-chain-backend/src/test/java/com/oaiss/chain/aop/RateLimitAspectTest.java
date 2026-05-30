package com.oaiss.chain.aop;

import com.oaiss.chain.annotation.RateLimit;
import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.dto.LoginRequest;
import com.oaiss.chain.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisTemplate<String, Long> redisScriptTemplate;

    private RateLimitAspect aspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    private Method testMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        aspect = new RateLimitAspect(redisTemplate, redisScriptTemplate);
        testMethod = TestService.class.getMethod("defaultLimitMethod");
        RequestContextHolder.resetRequestAttributes();
    }

    static class TestService {
        @RateLimit
        public String defaultLimitMethod() {
            return "result";
        }

        @RateLimit(key = "auth:login", limit = 5, period = 60, limitType = RateLimit.LimitType.IP_USER)
        public String login(LoginRequest request) {
            return "login";
        }
    }

    @Test
    void testRateLimitCheckPassed_UnderLimit() throws Throwable {
        Object expectedResult = "success";
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(testMethod);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(signature.getName()).thenReturn("defaultLimitMethod");
        when(redisScriptTemplate.execute(
                any(DefaultRedisScript.class),
                any(),
                anyString()
        )).thenReturn(50L);
        when(joinPoint.proceed()).thenReturn(expectedResult);

        Object result = aspect.enforceRateLimit(joinPoint);

        assertThat(result).isEqualTo(expectedResult);
        verify(redisScriptTemplate).execute(
                any(DefaultRedisScript.class),
                any(),
                anyString()
        );
        verify(joinPoint).proceed();
    }

    @Test
    void testRateLimitCheckFailed_OverLimit_ThrowsBusinessException() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(testMethod);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(signature.getName()).thenReturn("defaultLimitMethod");
        when(redisScriptTemplate.execute(
                any(DefaultRedisScript.class),
                any(),
                anyString()
        )).thenReturn(101L); // Over default limit (100)

        assertThatThrownBy(() -> aspect.enforceRateLimit(joinPoint))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.REQUEST_TOO_FREQUENT);

        verify(redisScriptTemplate).execute(
                any(DefaultRedisScript.class),
                any(),
                anyString()
        );
        verify(joinPoint, never()).proceed();
    }

    @Test
    void testIpUserRateLimitUsesUsernameForAnonymousLoginRequests() throws Throwable {
        Method loginMethod = TestService.class.getMethod("login", LoginRequest.class);
        LoginRequest loginRequest = LoginRequest.builder()
                .username("enterprise001")
                .password("admin123")
                .build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(loginMethod);
        when(joinPoint.getArgs()).thenReturn(new Object[]{loginRequest});
        when(redisScriptTemplate.execute(any(DefaultRedisScript.class), any(), anyString())).thenReturn(1L);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.enforceRateLimit(joinPoint);

        assertThat(result).isEqualTo("ok");
        verify(redisScriptTemplate).execute(
                any(DefaultRedisScript.class),
                eq(List.of("rate_limit:auth:login:ip:10.0.0.8:user:enterprise001")),
                eq("60")
        );
    }
}

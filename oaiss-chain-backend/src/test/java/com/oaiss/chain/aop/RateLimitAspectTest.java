package com.oaiss.chain.aop;

import com.oaiss.chain.annotation.RateLimit;
import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.exception.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private RateLimitAspect aspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    private Method testMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        testMethod = TestService.class.getMethod("defaultLimitMethod");
    }

    static class TestService {
        @RateLimit
        public String defaultLimitMethod() {
            return "result";
        }
    }

    @Test
    void testRateLimitCheckPassed_UnderLimit() throws Throwable {
        Object expectedResult = "success";
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(testMethod);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(signature.getName()).thenReturn("defaultLimitMethod");
        // Updated to mock redisTemplate.execute instead of valueOperations.increment
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                any(),
                anyString()
        )).thenReturn(50L);
        when(joinPoint.proceed()).thenReturn(expectedResult);

        Object result = aspect.enforceRateLimit(joinPoint);

        assertThat(result).isEqualTo(expectedResult);
        verify(redisTemplate).execute(
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
        // Updated to mock redisTemplate.execute instead of valueOperations.increment
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                any(),
                anyString()
        )).thenReturn(101L); // Over default limit (100)

        assertThatThrownBy(() -> aspect.enforceRateLimit(joinPoint))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.REQUEST_TOO_FREQUENT);

        verify(redisTemplate).execute(
                any(DefaultRedisScript.class),
                any(),
                anyString()
        );
        verify(joinPoint, never()).proceed();
    }
}

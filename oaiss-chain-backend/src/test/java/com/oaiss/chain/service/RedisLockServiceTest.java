package com.oaiss.chain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * RedisLockService 单元测试
 * RedisLockService Unit Tests
 */
@ExtendWith(MockitoExtension.class)
class RedisLockServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisLockService redisLockService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("尝试获取锁成功")
    void testTryLockSuccess() {
        // Given
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        // When
        String lockValue = redisLockService.tryLock("test-lock");

        // Then
        assertNotNull(lockValue);
        verify(valueOperations, times(1)).setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("尝试获取锁失败")
    void testTryLockFail() {
        // Given
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        // When
        String lockValue = redisLockService.tryLock("test-lock");

        // Then
        assertNull(lockValue);
    }

    @Test
    @DisplayName("尝试获取锁-自定义过期时间")
    void testTryLockWithCustomExpireTime() {
        // Given
        when(valueOperations.setIfAbsent(anyString(), anyString(), eq(60L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        // When
        String lockValue = redisLockService.tryLock("test-lock", 60, TimeUnit.SECONDS);

        // Then
        assertNotNull(lockValue);
    }

    @Test
    @DisplayName("释放锁成功")
    void testReleaseLockSuccess() {
        // Given
        String lockValue = "test-lock-value";
        when(valueOperations.get(anyString())).thenReturn(lockValue);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // When
        boolean result = redisLockService.releaseLock("test-lock", lockValue);

        // Then
        assertTrue(result);
        verify(redisTemplate, times(1)).delete(anyString());
    }

    @Test
    @DisplayName("释放锁失败-锁值不匹配")
    void testReleaseLockFailValueMismatch() {
        // Given
        String lockValue = "test-lock-value";
        when(valueOperations.get(anyString())).thenReturn("different-value");

        // When
        boolean result = redisLockService.releaseLock("test-lock", lockValue);

        // Then
        assertFalse(result);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("释放锁失败-锁值为null")
    void testReleaseLockFailNullValue() {
        // When
        boolean result = redisLockService.releaseLock("test-lock", null);

        // Then
        assertFalse(result);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("检查锁存在")
    void testIsLockedTrue() {
        // Given
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        // When
        boolean result = redisLockService.isLocked("test-lock");

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("检查锁不存在")
    void testIsLockedFalse() {
        // Given
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // When
        boolean result = redisLockService.isLocked("test-lock");

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("强制释放锁成功")
    void testForceReleaseLockSuccess() {
        // Given
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // When
        boolean result = redisLockService.forceReleaseLock("test-lock");

        // Then
        assertTrue(result);
        verify(redisTemplate, times(1)).delete(anyString());
    }

    @Test
    @DisplayName("强制释放锁失败")
    void testForceReleaseLockFail() {
        // Given
        when(redisTemplate.delete(anyString())).thenReturn(false);

        // When
        boolean result = redisLockService.forceReleaseLock("test-lock");

        // Then
        assertFalse(result);
    }
}

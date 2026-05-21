package com.oaiss.chain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.HashOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CachePreloadServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private CachePreloadService cachePreloadService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("缓存预热-用户类型缓存不存在时创建")
    void testPreloadUserTypeCacheNotExists() {
        // All caches don't exist
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);

        cachePreloadService.preloadCacheOnStartup();

        // Verify that expire was called for user_types cache (24 hours)
        verify(redisTemplate).expire(eq("user_types:all"), anyLong(), any());
    }

    @Test
    @DisplayName("缓存预热-用户类型缓存已存在时跳过")
    void testPreloadUserTypeCacheExists() {
        when(redisTemplate.hasKey("user_types:all")).thenReturn(true);

        cachePreloadService.preloadCacheOnStartup();

        verify(setOperations, never()).add(eq("user_types:all"), any());
    }

    @Test
    @DisplayName("缓存预热-所有缓存已存在时跳过创建")
    void testPreloadCacheAllExists() {
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        cachePreloadService.preloadCacheOnStartup();

        verify(setOperations, never()).add(anyString(), any());
        verify(hashOperations, never()).put(anyString(), any(), any());
    }

    @Test
    @DisplayName("手动刷新所有缓存")
    void testRefreshAllCaches() {
        Cache mockCache = mock(Cache.class);
        when(cacheManager.getCacheNames()).thenReturn(List.of("cache1", "cache2"));
        when(cacheManager.getCache("cache1")).thenReturn(mockCache);
        when(cacheManager.getCache("cache2")).thenReturn(mockCache);
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        cachePreloadService.refreshAllCaches();

        verify(mockCache, times(2)).clear();
    }

    @Test
    @DisplayName("手动刷新缓存-空缓存列表")
    void testRefreshAllCachesEmpty() {
        when(cacheManager.getCacheNames()).thenReturn(List.of());
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        cachePreloadService.refreshAllCaches();

        assertNotNull(cachePreloadService);
    }

    @Test
    @DisplayName("获取缓存统计信息")
    void testGetCacheStatistics() {
        when(cacheManager.getCacheNames()).thenReturn(List.of("cache1", "cache2"));
        when(redisTemplate.execute(any(RedisCallback.class))).thenAnswer(invocation -> 2L);

        CachePreloadService.CacheStatistics stats = cachePreloadService.getCacheStatistics();

        assertNotNull(stats);
        assertEquals(2, stats.totalCaches());
        assertTrue(stats.totalKeys() >= 0);
        verify(redisTemplate, atLeastOnce()).execute(any(RedisCallback.class));
    }

    @Test
    @DisplayName("获取缓存统计信息-空缓存")
    void testGetCacheStatisticsEmpty() {
        when(cacheManager.getCacheNames()).thenReturn(List.of());

        CachePreloadService.CacheStatistics stats = cachePreloadService.getCacheStatistics();

        assertNotNull(stats);
        assertEquals(0, stats.totalCaches());
        assertEquals(0, stats.totalKeys());
    }

    @Test
    @DisplayName("缓存统计摘要")
    void testCacheStatisticsSummary() {
        CachePreloadService.CacheStatistics stats = new CachePreloadService.CacheStatistics(5, 100);

        String summary = stats.toSummary();

        assertNotNull(summary);
        assertTrue(summary.contains("5"));
        assertTrue(summary.contains("100"));
    }

    @Test
    @DisplayName("缓存预热-异常处理")
    void testPreloadCacheWithException() {
        when(redisTemplate.hasKey("user_types:all")).thenReturn(false);
        when(setOperations.add(anyString(), any())).thenThrow(new RuntimeException("Redis error"));

        assertDoesNotThrow(() -> cachePreloadService.preloadCacheOnStartup());
    }
}
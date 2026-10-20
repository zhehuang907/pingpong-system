package com.pingpong.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RateLimitInterceptor rateLimitInterceptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rateLimitInterceptor, "enabled", true);
        ReflectionTestUtils.setField(rateLimitInterceptor, "generalWindowSeconds", 60L);
        ReflectionTestUtils.setField(rateLimitInterceptor, "generalMaxRequests", 1L);
        ReflectionTestUtils.setField(rateLimitInterceptor, "sensitiveWindowSeconds", 60L);
        ReflectionTestUtils.setField(rateLimitInterceptor, "sensitiveMaxRequests", 1L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.expire(anyString(), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);
    }

    @Test
    void preHandleShouldNotShareGeneralBucketAcrossDifferentUris() throws Exception {
        Map<String, Long> counters = new HashMap<>();
        when(valueOperations.increment(anyString(), eq(1L))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            long next = counters.getOrDefault(key, 0L) + 1L;
            counters.put(key, next);
            return next;
        });

        MockHttpServletRequest equipmentListRequest = new MockHttpServletRequest("GET", "/equipment/list");
        equipmentListRequest.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse equipmentListResponse = new MockHttpServletResponse();

        MockHttpServletRequest favoriteCheckRequest = new MockHttpServletRequest("GET", "/favorite/check/1");
        favoriteCheckRequest.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse favoriteCheckResponse = new MockHttpServletResponse();

        boolean firstAllowed = rateLimitInterceptor.preHandle(equipmentListRequest, equipmentListResponse, new Object());
        boolean secondAllowed = rateLimitInterceptor.preHandle(favoriteCheckRequest, favoriteCheckResponse, new Object());

        assertTrue(firstAllowed);
        assertTrue(secondAllowed);
        assertEquals(200, equipmentListResponse.getStatus());
        assertEquals(200, favoriteCheckResponse.getStatus());
    }

    @Test
    void preHandleShouldStillLimitRepeatedRequestsToTheSameUri() throws Exception {
        Map<String, Long> counters = new HashMap<>();
        when(valueOperations.increment(anyString(), eq(1L))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            long next = counters.getOrDefault(key, 0L) + 1L;
            counters.put(key, next);
            return next;
        });

        MockHttpServletRequest firstRequest = new MockHttpServletRequest("GET", "/leaderboard/home");
        firstRequest.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();

        MockHttpServletRequest secondRequest = new MockHttpServletRequest("GET", "/leaderboard/home");
        secondRequest.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();

        boolean firstAllowed = rateLimitInterceptor.preHandle(firstRequest, firstResponse, new Object());
        boolean secondAllowed = rateLimitInterceptor.preHandle(secondRequest, secondResponse, new Object());

        assertTrue(firstAllowed);
        assertEquals(200, firstResponse.getStatus());
        assertEquals(false, secondAllowed);
        assertEquals(429, secondResponse.getStatus());
    }
}

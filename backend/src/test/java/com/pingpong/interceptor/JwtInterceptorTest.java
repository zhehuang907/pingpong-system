package com.pingpong.interceptor;

import com.pingpong.util.JwtUtil;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private JwtInterceptor jwtInterceptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtInterceptor, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(jwtInterceptor, "redisTemplate", redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void preHandleShouldAcceptStandardBearerHeaderWithWhitespace() throws Exception {
        when(jwtUtil.getHeader()).thenReturn("Authorization");
        when(jwtUtil.getPrefix()).thenReturn("Bearer");
        when(jwtUtil.validateToken("token-123")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("token-123")).thenReturn(3L);
        when(jwtUtil.getUsernameFromToken("token-123")).thenReturn("PaddleAce_26");
        when(valueOperations.get("user:token:3")).thenReturn("token-123");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.addHeader("Authorization", "Bearer token-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = jwtInterceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
        assertEquals(3L, request.getAttribute("userId"));
        assertEquals("PaddleAce_26", request.getAttribute("username"));
    }
}

package com.pingpong.interceptor;

import com.pingpong.common.BaseContext;
import com.pingpong.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader(jwtUtil.getHeader());
        if (!StringUtils.hasText(authorization)) {
            // No token provided? Allow guest access (will be handled by controller/admin checks if protected)
            return true;
        }

        String token = resolveToken(authorization);
        if (!StringUtils.hasText(token)) {
            responseUnauthorized(response, "Not logged in or session expired");
            return false;
        }

        if (!jwtUtil.validateToken(token)) {
            responseUnauthorized(response, "Login session expired");
            return false;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        if (!validateTokenWithRedis(userId, token)) {
            responseUnauthorized(response, "Not logged in or session expired");
            return false;
        }

        // 设置当前用户ID到BaseContext
        BaseContext.setCurrentId(userId);

        request.setAttribute("userId", userId);
        request.setAttribute("username", jwtUtil.getUsernameFromToken(token));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理线程局部变量
        BaseContext.removeCurrentId();
    }

    private String resolveToken(String authorization) {
        String prefix = jwtUtil.getPrefix();
        if (!StringUtils.hasText(prefix)) {
            return authorization.trim();
        }

        if (authorization.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return authorization.substring(prefix.length()).trim();
        }

        return null;
    }

    private boolean validateTokenWithRedis(Long userId, String token) {
        String key = "user:token:" + userId;
        try {
            String redisToken = redisTemplate.opsForValue().get(key);
            if (redisToken == null) {
                return true;
            }
            if (!redisToken.equals(token)) {
                return false;
            }
            redisTemplate.expire(key, jwtUtil.getExpireTime(), TimeUnit.SECONDS);
            return true;
        } catch (Exception ignored) {
            // Allow JWT-only validation when Redis is unavailable in local/dev setups.
            return true;
        }
    }

    private void responseUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.write("{\"code\":401,\"msg\":\"" + escapeJson(message) + "\",\"data\":null}");
            out.flush();
        }
    }

    private String escapeJson(String message) {
        return message.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

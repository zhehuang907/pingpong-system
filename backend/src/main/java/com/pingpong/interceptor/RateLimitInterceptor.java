package com.pingpong.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String GENERAL_BUCKET = "general";
    private static final String SENSITIVE_BUCKET = "sensitive";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${security.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${security.rate-limit.window-seconds:60}")
    private long generalWindowSeconds;

    @Value("${security.rate-limit.max-requests:120}")
    private long generalMaxRequests;

    @Value("${security.rate-limit.sensitive-window-seconds:60}")
    private long sensitiveWindowSeconds;

    @Value("${security.rate-limit.sensitive-max-requests:12}")
    private long sensitiveMaxRequests;

    private static final List<String> SENSITIVE_PATTERNS = Arrays.asList(
            "/user/login",
            "/user/register",
            "/user/forgot-password",
            "/captcha/**"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!enabled || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        if (isIgnoredPath(uri)) {
            return true;
        }

        String bucket = isSensitivePath(uri) ? SENSITIVE_BUCKET : GENERAL_BUCKET;
        long max = SENSITIVE_BUCKET.equals(bucket) ? sensitiveMaxRequests : generalMaxRequests;
        long windowSeconds = SENSITIVE_BUCKET.equals(bucket) ? sensitiveWindowSeconds : generalWindowSeconds;

        String ip = resolveClientIp(request);
        String key = buildRateKey(ip, bucket, uri, request.getMethod());

        try {
            Long current = redisTemplate.opsForValue().increment(key, 1L);
            if (current != null && current == 1L) {
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }
            if (current != null && current > max) {
                responseRateLimited(response);
                return false;
            }
        } catch (Exception ignored) {
            // When Redis is unavailable, degrade to no-limiting to avoid breaking local/dev runs.
            return true;
        }
        return true;
    }

    private boolean isSensitivePath(String uri) {
        return SENSITIVE_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }

    private boolean isIgnoredPath(String uri) {
        return uri.startsWith("/uploads/")
                || uri.startsWith("/ws/")
                || uri.startsWith("/doc.html")
                || uri.startsWith("/webjars/")
                || uri.startsWith("/swagger-resources/")
                || uri.startsWith("/v2/api-docs");
    }

    private String buildRateKey(String ip, String bucket, String uri, String method) {
        // Keep independent counters per endpoint so a page's normal parallel GET requests
        // do not exhaust the entire IP-wide bucket and trigger false 429s.
        return "rate:limit:" + bucket + ":" + ip + ":" + method + ":" + uri;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            int commaIndex = xForwardedFor.indexOf(',');
            return commaIndex > 0 ? xForwardedFor.substring(0, commaIndex).trim() : xForwardedFor.trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void responseRateLimited(HttpServletResponse response) throws Exception {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.write("{\"code\":429,\"msg\":\"Too many requests, please retry later.\",\"data\":null}");
            out.flush();
        }
    }
}

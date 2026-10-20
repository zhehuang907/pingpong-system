package com.pingpong.aspect;

import cn.hutool.json.JSONUtil;
import com.pingpong.entity.BehaviorLog;
import com.pingpong.entity.CartItem;
import com.pingpong.entity.Comment;
import com.pingpong.mapper.BehaviorLogMapper;
import com.pingpong.mapper.CartItemMapper;
import com.pingpong.mapper.CommentMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private BehaviorLogMapper behaviorLogMapper;

    @Autowired(required = false)
    private CommentMapper commentMapper;

    @Autowired(required = false)
    private CartItemMapper cartItemMapper;

    @Around("execution(* com.pingpong.controller..*(..))")
    public Object aroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();

        long start = System.currentTimeMillis();
        Object result = null;
        String resultText = "SUCCESS";
        Throwable throwable = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            throwable = ex;
            resultText = "FAILED: " + ex.getClass().getSimpleName();
            throw ex;
        } finally {
            if (!shouldSkip(request)) {
                BehaviorLog logRecord = buildBehaviorLogRecord(request, joinPoint, result, throwable, resultText, start);
                if (logRecord != null) {
                    try {
                        behaviorLogMapper.insert(logRecord);
                    } catch (Exception saveEx) {
                        log.warn("Failed to persist operation log: {}", saveEx.getMessage());
                    }
                }
            }
        }
    }

    private boolean shouldSkip(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String method = request.getMethod();
        String uri = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method) && !uri.startsWith("/admin/")) {
            return true;
        }
        return uri.startsWith("/uploads/")
                || uri.startsWith("/ws/")
                || uri.startsWith("/captcha/");
    }

    private BehaviorLog buildBehaviorLogRecord(HttpServletRequest request,
                                               ProceedingJoinPoint joinPoint,
                                               Object result,
                                               Throwable throwable,
                                               String resultText,
                                               long start) {
        if (request == null) {
            return null;
        }

        Long userId = resolveUserId(request);
        Long equipmentId = resolveEquipmentId(request, joinPoint.getArgs());
        String behaviorType = resolveBehaviorType(request.getRequestURI());
        if (userId == null || equipmentId == null || !StringUtils.hasText(behaviorType)) {
            return null;
        }

        BehaviorLog logRecord = new BehaviorLog();
        logRecord.setUserId(userId);
        logRecord.setEquipmentId(equipmentId);

        Object username = request.getAttribute("username");
        if (username != null) {
            logRecord.setUsername(String.valueOf(username));
        }
        logRecord.setBehaviorType(behaviorType);
        logRecord.setWeight(resolveWeight(behaviorType));
        logRecord.setIp(resolveClientIp(request));
        logRecord.setUrl(request.getRequestURI());
        logRecord.setMethod(request.getMethod());
        logRecord.setUserAgent(trim(request.getHeader("User-Agent"), 255));
        logRecord.setModule(resolveModule(request.getRequestURI()));

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        logRecord.setOperation(signature.getDeclaringType().getSimpleName() + "." + signature.getMethod().getName());
        logRecord.setParams(trim(safeJson(joinPoint.getArgs()), 2000));
        if (throwable == null) {
            logRecord.setResult(trim(safeJson(result), 1000));
        } else {
            logRecord.setResult(trim(resultText, 1000));
        }
        logRecord.setTimeTaken(System.currentTimeMillis() - start);
        return logRecord;
    }

    private Long resolveUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId instanceof Long) {
            return (Long) userId;
        }
        if (userId instanceof String && StringUtils.hasText((String) userId)) {
            try {
                return Long.valueOf((String) userId);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Long resolveEquipmentId(HttpServletRequest request, Object[] args) {
        Long fromArgs = extractEquipmentIdFromArgs(args);
        if (fromArgs != null) {
            return fromArgs;
        }

        String uri = request.getRequestURI();
        if (!StringUtils.hasText(uri)) {
            return null;
        }

        if (uri.contains("/cart/add/")
                || uri.contains("/favorite/toggle/")
                || uri.contains("/favorite/cancel/")
                || uri.contains("/history/record/")
                || uri.contains("/recommend/click/")) {
            return extractLastNumericSegment(uri);
        }

        if (uri.contains("/comment/like/") || ("DELETE".equalsIgnoreCase(request.getMethod()) && uri.matches(".*/comment/\\d+$"))) {
            Long commentId = extractLastNumericSegment(uri);
            if (commentId != null && commentMapper != null) {
                Comment comment = commentMapper.selectById(commentId);
                return comment == null ? null : comment.getEquipmentId();
            }
        }

        if (uri.contains("/cart/")) {
            Long cartItemId = extractLastNumericSegment(uri);
            if (cartItemId != null && cartItemMapper != null) {
                CartItem cartItem = cartItemMapper.selectById(cartItemId);
                return cartItem == null ? null : cartItem.getEquipmentId();
            }
        }

        return null;
    }

    private Long extractEquipmentIdFromArgs(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            if (arg instanceof Number) {
                continue;
            }
            if (arg instanceof HttpServletRequest) {
                continue;
            }
            try {
                Method method = arg.getClass().getMethod("getEquipmentId");
                Object value = method.invoke(arg);
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
            } catch (Exception ignored) {
                // ignored
            }
        }
        return null;
    }

    private Long extractLastNumericSegment(String uri) {
        if (!StringUtils.hasText(uri)) {
            return null;
        }
        String[] segments = uri.split("/");
        for (int i = segments.length - 1; i >= 0; i--) {
            String segment = segments[i];
            if (!StringUtils.hasText(segment)) {
                continue;
            }
            try {
                return Long.valueOf(segment);
            } catch (NumberFormatException ignored) {
                // ignored
            }
        }
        return null;
    }

    private String resolveBehaviorType(String uri) {
        if (!StringUtils.hasText(uri)) {
            return null;
        }
        if (uri.contains("/history/record") || uri.contains("/recommend/click")) {
            return "BROWSE";
        }
        if (uri.contains("/favorite/")) {
            return "FAVORITE";
        }
        if (uri.contains("/comment")) {
            return "COMMENT";
        }
        if (uri.contains("/cart/")) {
            return "CART";
        }
        return "INTERACT";
    }

    private int resolveWeight(String behaviorType) {
        if (!StringUtils.hasText(behaviorType)) {
            return 1;
        }
        switch (behaviorType) {
            case "COMMENT":
                return 5;
            case "FAVORITE":
                return 3;
            case "CART":
                return 2;
            default:
                return 1;
        }
    }

    private String resolveModule(String uri) {
        if (!StringUtils.hasText(uri)) {
            return "unknown";
        }
        String cleaned = uri.startsWith("/") ? uri.substring(1) : uri;
        int slashIndex = cleaned.indexOf('/');
        return slashIndex > 0 ? cleaned.substring(0, slashIndex) : cleaned;
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

    private String safeJson(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            if (value.getClass().isArray()) {
                return Arrays.stream((Object[]) value).map(this::safeJson).collect(Collectors.joining(","));
            }
            return JSONUtil.toJsonStr(value);
        } catch (Exception ignored) {
            return String.valueOf(value);
        }
    }

    private String trim(String text, int maxLen) {
        if (!StringUtils.hasText(text) || text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen);
    }
}

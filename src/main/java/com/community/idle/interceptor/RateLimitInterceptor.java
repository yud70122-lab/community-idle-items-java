package com.community.idle.interceptor;

import com.community.idle.annotation.RateLimit;
import com.community.idle.utils.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    private static final int TOO_MANY_REQUESTS = 429;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();

        RateLimit methodAnnotation = method.getAnnotation(RateLimit.class);
        RateLimit classAnnotation = handlerMethod.getBeanType().getAnnotation(RateLimit.class);

        RateLimit rateLimit = methodAnnotation != null ? methodAnnotation : classAnnotation;
        if (rateLimit == null) {
            return true;
        }

        return checkRateLimit(request, response, rateLimit);
    }

    private boolean checkRateLimit(HttpServletRequest request, HttpServletResponse response, RateLimit rateLimit) {
        String key = generateKey(request, rateLimit);

        int period = rateLimit.period();
        TimeUnit timeUnit = rateLimit.timeUnit();
        long expireSeconds = timeUnit.toSeconds(period);
        int limit = rateLimit.limit();

        try {
            String redisKey = RATE_LIMIT_KEY_PREFIX + key;

            Long currentCount = redisUtil.incr(redisKey, 1);

            if (currentCount != null && currentCount == 1) {
                redisUtil.expire(redisKey, expireSeconds);
            }

            if (currentCount != null && currentCount > limit) {
                log.warn("Rate limit exceeded: key={}, count={}, limit={}/{}s",
                        key, currentCount, limit, expireSeconds);
                writeErrorResponse(response, rateLimit.message());
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Rate limit check error, key={}", key, e);
            return true;
        }
    }

    private String generateKey(HttpServletRequest request, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder();

        keyBuilder.append(getMethodPath(request));

        switch (rateLimit.type()) {
            case IP:
                keyBuilder.append(":ip:").append(getClientIp(request));
                break;
            case USER_ID:
                keyBuilder.append(":user:").append(getCurrentUserId());
                break;
            case IP_AND_USER_ID:
                keyBuilder.append(":ip:").append(getClientIp(request))
                        .append(":user:").append(getCurrentUserId());
                break;
        }

        if (!rateLimit.key().isEmpty()) {
            keyBuilder.append(":").append(rateLimit.key());
        }

        return keyBuilder.toString();
    }

    private String getMethodPath(HttpServletRequest request) {
        return request.getMethod().toLowerCase() + ":" + request.getRequestURI();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }

    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof Long) {
                return authentication.getPrincipal().toString();
            }
        } catch (Exception e) {
            log.debug("Cannot get current user id", e);
        }
        return "anonymous";
    }

    private void writeErrorResponse(HttpServletResponse response, String message) {
        response.setStatus(TOO_MANY_REQUESTS);
        response.setContentType("application/json;charset=UTF-8");
        try {
            String json = String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null}",
                    TOO_MANY_REQUESTS, message);
            response.getWriter().write(json);
        } catch (IOException e) {
            log.error("Error writing rate limit response", e);
        }
    }
}

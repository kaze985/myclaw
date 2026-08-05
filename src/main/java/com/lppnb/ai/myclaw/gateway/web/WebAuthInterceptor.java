package com.lppnb.ai.myclaw.gateway.web;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Web 通道认证拦截器：校验请求携带的 HttpOnly cookie（myclaw_session）是否为有效会话。
 */
public class WebAuthInterceptor implements HandlerInterceptor {

    /** 会话 cookie 名称 */
    public static final String SESSION_COOKIE = "myclaw_session";

    private final WebSessionRegistry sessionRegistry;

    public WebAuthInterceptor(WebSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String sessionId = extractSessionId(request);
        if (sessionRegistry.isValid(sessionId)) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }

    /** 从请求 cookie 中提取会话 ID，无 cookie 时返回 null */
    public static String extractSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (SESSION_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}

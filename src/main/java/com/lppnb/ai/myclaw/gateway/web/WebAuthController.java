package com.lppnb.ai.myclaw.gateway.web;

import java.time.Duration;
import java.util.Map;

import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Web 通道认证端点：密码登录签发 HttpOnly cookie、登出、认证态探测。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Conditional(WebChannelCondition.class)
public class WebAuthController {

    private final WebProperties webProperties;
    private final WebSessionRegistry sessionRegistry;

    /** 登录：校验密码，成功签发 HttpOnly cookie */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String password = body == null ? null : body.get("password");
        if (webProperties.getAccessPassword() == null
                || !webProperties.getAccessPassword().equals(password)) {
            return ResponseEntity.status(401).body(Map.of("message", "密码错误"));
        }
        String sessionId = sessionRegistry.create();
        response.addHeader(HttpHeaders.SET_COOKIE, buildSessionCookie(sessionId, Duration.ofDays(7)).toString());
        return ResponseEntity.ok(Map.of("message", "登录成功"));
    }

    /** 登出：注销服务端会话并清除客户端 cookie */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {
        sessionRegistry.invalidate(WebAuthInterceptor.extractSessionId(request));
        response.addHeader(HttpHeaders.SET_COOKIE, buildSessionCookie("", Duration.ZERO).toString());
        return ResponseEntity.ok(Map.of("message", "已登出"));
    }

    /** 认证态探测：有效会话返回 200，否则 401 */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Boolean>> me(HttpServletRequest request) {
        boolean authenticated = sessionRegistry.isValid(WebAuthInterceptor.extractSessionId(request));
        if (!authenticated) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }
        return ResponseEntity.ok(Map.of("authenticated", true));
    }

    private ResponseCookie buildSessionCookie(String sessionId, Duration maxAge) {
        return ResponseCookie.from(WebAuthInterceptor.SESSION_COOKIE, sessionId)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/api")
                .maxAge(maxAge)
                .build();
    }
}

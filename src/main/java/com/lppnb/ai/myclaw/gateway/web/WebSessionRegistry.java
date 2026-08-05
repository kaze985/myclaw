package com.lppnb.ai.myclaw.gateway.web;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * Web 会话注册表：内存会话表（{@code sessionId -> 过期时间戳}）。
 * 应用重启后所有会话失效，用户需重新登录（MVP 接受）。
 */
@Component
@Conditional(WebChannelCondition.class)
public class WebSessionRegistry {

    /** 会话有效期：7 天 */
    public static final long SESSION_TTL_MS = 7L * 24 * 60 * 60 * 1000;

    /** 会话表（sessionId -> 过期时间戳）；包可见以便测试 */
    final Map<String, Long> sessions = new ConcurrentHashMap<>();

    /** 签发一个新会话，返回会话 ID */
    public String create() {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        sessions.put(sessionId, System.currentTimeMillis() + SESSION_TTL_MS);
        return sessionId;
    }

    /** 会话是否有效（存在且未过期）；过期会话自动清除 */
    public boolean isValid(String sessionId) {
        if (sessionId == null) {
            return false;
        }
        Long expiresAt = sessions.get(sessionId);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt < System.currentTimeMillis()) {
            sessions.remove(sessionId);
            return false;
        }
        return true;
    }

    /** 注销会话 */
    public void invalidate(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }
}

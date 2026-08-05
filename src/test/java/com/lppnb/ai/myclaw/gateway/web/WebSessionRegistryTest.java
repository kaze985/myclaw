package com.lppnb.ai.myclaw.gateway.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Web 会话注册表测试：签发、校验、注销与过期。
 */
class WebSessionRegistryTest {

    private final WebSessionRegistry registry = new WebSessionRegistry();

    @Test
    void createThenValidateSucceeds() {
        String sessionId = registry.create();
        assertTrue(registry.isValid(sessionId));
    }

    @Test
    void unknownSessionIsInvalid() {
        assertFalse(registry.isValid("no-such-session"));
        assertFalse(registry.isValid(null));
    }

    @Test
    void invalidateMakesSessionInvalid() {
        String sessionId = registry.create();
        registry.invalidate(sessionId);
        assertFalse(registry.isValid(sessionId));
    }

    @Test
    void expiredSessionIsInvalidAndPurged() {
        String sessionId = registry.create();
        // 将过期时间拨回过去，模拟会话过期
        registry.sessions.put(sessionId, System.currentTimeMillis() - 1000);
        assertFalse(registry.isValid(sessionId));
        assertFalse(registry.sessions.containsKey(sessionId), "过期会话应从表中清除");
    }
}

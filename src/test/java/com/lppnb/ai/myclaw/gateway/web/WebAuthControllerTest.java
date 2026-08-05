package com.lppnb.ai.myclaw.gateway.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Web 认证端点测试：密码登录、会话探测、登出。
 */
class WebAuthControllerTest {

    private MockMvc mockMvc;
    private WebSessionRegistry sessionRegistry;

    @BeforeEach
    void setUp() {
        WebProperties properties = new WebProperties();
        properties.setAccessPassword("secret");
        sessionRegistry = new WebSessionRegistry();
        mockMvc = MockMvcBuilders.standaloneSetup(new WebAuthController(properties, sessionRegistry)).build();
    }

    @Test
    void loginWithCorrectPasswordReturnsCookie() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(WebAuthInterceptor.SESSION_COOKIE))
                .andExpect(cookie().httpOnly(WebAuthInterceptor.SESSION_COOKIE, true));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithoutCookieReturns401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithValidSessionReturnsAuthenticated() throws Exception {
        String sessionId = sessionRegistry.create();
        mockMvc.perform(get("/auth/me").cookie(new jakarta.servlet.http.Cookie(WebAuthInterceptor.SESSION_COOKIE, sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        String sessionId = sessionRegistry.create();
        mockMvc.perform(post("/auth/logout").cookie(new jakarta.servlet.http.Cookie(WebAuthInterceptor.SESSION_COOKIE, sessionId)))
                .andExpect(status().isOk());
        // 登出后原会话应失效
        mockMvc.perform(get("/auth/me").cookie(new jakarta.servlet.http.Cookie(WebAuthInterceptor.SESSION_COOKIE, sessionId)))
                .andExpect(status().isUnauthorized());
    }
}

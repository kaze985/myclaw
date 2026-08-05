package com.lppnb.ai.myclaw.gateway.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.lppnb.ai.myclaw.gateway.channel.GatewayMessage;
import com.lppnb.ai.myclaw.gateway.channel.MessageRouter;

/**
 * Web 聊天端点测试：SSE 流式对话与会话控制（/chat/new）。
 */
class ChatControllerTest {

    private final MessageRouter messageRouter = mock(MessageRouter.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(messageRouter)).build();

    @Test
    void newChatResetsContext() throws Exception {
        when(messageRouter.route(any(GatewayMessage.class))).thenReturn("清空上下文成功！");
        mockMvc.perform(post("/chat/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("清空上下文成功！"));
        verify(messageRouter).route(any(GatewayMessage.class));
    }

    @Test
    void chatStreamsSseUntilDone() throws Exception {
        when(messageRouter.route(any(GatewayMessage.class))).thenReturn("hello world");
        MvcResult mvcResult = mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:done")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("hello world")));
        verify(messageRouter).route(any(GatewayMessage.class));
    }

    @Test
    void chatForwardsThoughtEvents() throws Exception {
        when(messageRouter.route(any(GatewayMessage.class))).thenAnswer(invocation -> {
            GatewayMessage message = invocation.getArgument(0);
            if (message.getOnThought() != null) {
                message.getOnThought().accept("正在调用 webSearch...");
            }
            return "final";
        });
        MvcResult mvcResult = mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"搜索\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        String body = mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("event:thought"), "应推送 thought 事件: " + body);
        assertTrue(body.contains("正在调用 webSearch"), "thought 事件应包含思考文本: " + body);
    }
}

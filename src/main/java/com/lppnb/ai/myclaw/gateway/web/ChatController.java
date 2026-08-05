package com.lppnb.ai.myclaw.gateway.web;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.lppnb.ai.myclaw.gateway.channel.GatewayMessage;
import com.lppnb.ai.myclaw.gateway.channel.MessageRouter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Web 通道聊天端点：SSE 流式对话与会话控制。
 *
 * <p>通过 {@link SseEmitter} 返回 SSE 流：Agent 每次思考（onThought）推送
 * {@code thought} 事件，执行完成推送 {@code done}，异常推送 {@code error}。
 * Agent 调用在独立线程中执行（与飞书通道一致），不阻塞 servlet 线程。</p>
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Conditional(WebChannelCondition.class)
public class ChatController {

    /** SseEmitter 超时：10 分钟 */
    private static final long SSE_TIMEOUT_MS = 10 * 60 * 1000L;

    private final MessageRouter messageRouter;

    /** 请求体：用户消息 */
    public record ChatRequest(String message) {
    }

    /** SSE 流式对话：返回 text/event-stream */
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> emitter.complete());

        String sessionId = WebAuthInterceptor.extractSessionId(httpRequest);
        String content = request == null ? null : request.message();

        // 记录最后一次 think 文本：Agent 的最终回复在最后一次 think 产生（route 返回空字符串），
        // done 事件用它作为 content 回退
        StringBuilder lastThought = new StringBuilder();

        GatewayMessage gatewayMessage = GatewayMessage.builder()
                .platform("web")
                .sessionId(sessionId)
                .senderId(sessionId)
                .content(content)
                .onThought(text -> {
                    lastThought.setLength(0);
                    lastThought.append(text);
                    sendEvent(emitter, "thought", Map.of("text", text));
                })
                .onToken(text -> sendEvent(emitter, "token", Map.of("text", text)))
                .build();

        CompletableFuture.runAsync(() -> {
            try {
                String result = messageRouter.route(gatewayMessage);
                String reply = StringUtils.isNotBlank(result) ? result : lastThought.toString();
                sendEvent(emitter, "done", Map.of("content", reply));
            } catch (Exception e) {
                log.error("Web chat failed for session={}: {}", sessionId, e.getMessage(), e);
                sendEvent(emitter, "error", Map.of("message", "处理消息时发生错误：" + e.getMessage()));
            } finally {
                emitter.complete();
            }
        });
        return emitter;
    }

    /** 新建会话：清空 Agent 上下文（等价于飞书 /new 命令） */
    @PostMapping("/new")
    public Map<String, String> newChat(HttpServletRequest httpRequest) {
        GatewayMessage gatewayMessage = GatewayMessage.builder()
                .platform("web")
                .sessionId(WebAuthInterceptor.extractSessionId(httpRequest))
                .content("/new")
                .build();
        String result = messageRouter.route(gatewayMessage);
        return Map.of("message", result);
    }

    /** 推送 SSE 事件；客户端已断开时记录日志（后续 complete 兜底） */
    private void sendEvent(SseEmitter emitter, String eventName, Map<String, String> data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
            log.debug("Web SSE event [{}] sent for session={}", eventName,
                    data.getOrDefault("content", data.getOrDefault("text", "")));
        } catch (Exception e) {
            log.warn("Failed to send SSE event {} (client may be gone): {}", eventName, e.getMessage());
        }
    }
}

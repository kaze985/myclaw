package com.lppnb.ai.myclaw.gateway.feishu;

import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lark.oapi.Client;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.service.im.v1.model.ReplyMessageReq;
import com.lark.oapi.service.im.v1.model.ReplyMessageReqBody;
import com.lark.oapi.service.im.v1.model.ReplyMessageResp;
import com.lppnb.ai.myclaw.gateway.channel.GatewayMessage;
import com.lppnb.ai.myclaw.gateway.channel.MessageRouter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 飞书事件处理器，封装 EventDispatcher 并注册消息接收处理逻辑。
 * 采用异步方式处理 Agent 调用，确保在飞书 3 秒内完成 ACK。
 */
@Slf4j
@RequiredArgsConstructor
public class FeishuEventHandler {

    private final MessageRouter messageRouter;

    private EventDispatcher eventDispatcher;

    /**
     * 飞书主客户端（在 FeishuChannel 启动后注入）
     */
    private Client larkClient;

    public void init(Client larkClient) {
        this.larkClient = larkClient;
        this.eventDispatcher = EventDispatcher.newBuilder("", "")
                .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                    @Override
                    public void handle(P2MessageReceiveV1 event) throws Exception {
                        handleMessageReceive(event);
                    }
                })
                .build();
    }

    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    private void handleMessageReceive(P2MessageReceiveV1 event) {
        var message = event.getEvent().getMessage();
        String msgType = message.getMessageType();
        String messageId = message.getMessageId();
        String openId = event.getEvent().getSender().getSenderId().getOpenId();

        if (!"text".equals(msgType)) {
            log.debug("Skipping non-text message type={} messageId={}", msgType, messageId);
            return;
        }

        String rawContent = message.getContent();
        String textContent = extractText(rawContent);

        GatewayMessage gatewayMessage = GatewayMessage.builder()
                .platform("feishu")
                .sessionId(messageId)
                .senderId(openId)
                .content(textContent)
                .rawPayload(event)
                .onThought(thought -> replyMessage(messageId, thought))
                .build();

        CompletableFuture.runAsync(() -> {
            try {
                String agentLoopResults = messageRouter.route(gatewayMessage);
                // route() 返回的是agent loop中每一步step的汇总结果，模型thought已通过回调逐条发送，所以此处就不发送了
                log.debug("Agent run completed for messageId={}, agentLoopResults length={}", messageId, agentLoopResults == null ? 0 : agentLoopResults.length());
            } catch (Exception e) {
                log.error("Async agent processing failed for messageId={}: {}", messageId, e.getMessage(), e);
                replyMessage(messageId, "抱歉，处理您的消息时发生异常：" + e.getMessage());
            }
        });
    }

    /**
     * 从飞书 JSON content 中提取文本，格式为 {"text":"..."}
     */
    private String extractText(String content) {
        try {
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            return json.get("text").getAsString();
        } catch (Exception e) {
            log.warn("Failed to parse message content as text JSON, using raw: {}", content);
            return content;
        }
    }

    /**
     * 将文本内容序列化为飞书文本消息 JSON
     */
    private String toTextContent(String text) {
        JsonObject json = new JsonObject();
        json.addProperty("text", text);
        return json.toString();
    }

    /**
     * 通过飞书 API 回复消息
     */
    private void replyMessage(String messageId, String content) {
        if (larkClient == null) {
            log.error("LarkClient is not initialized, cannot reply to messageId={}", messageId);
            return;
        }
        try {
            ReplyMessageResp resp = larkClient.im().v1().message().reply(
                    ReplyMessageReq.newBuilder()
                            .messageId(messageId)
                            .replyMessageReqBody(ReplyMessageReqBody.newBuilder()
                                    .msgType("text")
                                    .content(toTextContent(content))
                                    .build())
                            .build()
            );
            if (!resp.success()) {
                log.error("Failed to reply message, code={} msg={} reqId={}",
                        resp.getCode(), resp.getMsg(), resp.getRequestId());
            } else {
                log.debug("Reply sent successfully for messageId={}", messageId);
            }
        } catch (Exception e) {
            log.error("Exception while replying message to messageId={}: {}", messageId, e.getMessage(), e);
        }
    }
}

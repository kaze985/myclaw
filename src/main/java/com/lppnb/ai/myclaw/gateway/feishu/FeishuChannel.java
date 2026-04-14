package com.lppnb.ai.myclaw.gateway.feishu;

import com.google.gson.JsonObject;
import com.lark.oapi.Client;
import com.lark.oapi.service.im.v1.model.CreateMessageReq;
import com.lark.oapi.service.im.v1.model.CreateMessageReqBody;
import com.lark.oapi.service.im.v1.model.CreateMessageResp;
import com.lppnb.ai.myclaw.gateway.channel.Channel;
import com.lppnb.ai.myclaw.gateway.channel.GatewayMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 飞书 Channel 实现，基于飞书 Java SDK 长连接（WebSocket）模式接收消息并通过消息 API 回复。
 */
@Slf4j
@RequiredArgsConstructor
public class FeishuChannel implements Channel {

    private final FeishuProperties properties;
    private final FeishuEventHandler eventHandler;

    private Client larkClient;
    private com.lark.oapi.ws.Client wsClient;

    @Override
    public void start() {
        log.info("Starting Feishu Channel (WebSocket mode), appId={}", properties.getAppId());

        larkClient = Client.newBuilder(properties.getAppId(), properties.getAppSecret()).build();
        eventHandler.init(larkClient);

        wsClient = new com.lark.oapi.ws.Client.Builder(properties.getAppId(), properties.getAppSecret())
                .eventHandler(eventHandler.getEventDispatcher())
                .build();

        Thread daemonThread = new Thread(() -> {
            try {
                wsClient.start();
            } catch (Exception e) {
                log.error("Feishu WebSocket client encountered an error: {}", e.getMessage(), e);
            }
        });
        daemonThread.setName("feishu-ws-client");
        daemonThread.setDaemon(true);
        daemonThread.start();

        log.info("Feishu Channel started, WebSocket daemon thread launched.");
    }

    @Override
    public void stop() {
        log.info("Stopping Feishu Channel...");
        if (wsClient != null) {
            try {
                wsClient = null;
                log.info("Feishu WebSocket connection closed.");
            } catch (Exception e) {
                log.warn("Exception while closing Feishu WebSocket: {}", e.getMessage(), e);
            } catch (Throwable t) {
                log.warn("Unexpected error while closing Feishu WebSocket: {}", t.getMessage());
            }
        }
    }

    @Override
    public void send(GatewayMessage message) {
        if (larkClient == null) {
            log.error("LarkClient is not initialized, cannot send message.");
            return;
        }
        try {
            JsonObject content = new JsonObject();
            content.addProperty("text", message.getContent());
            CreateMessageResp resp = larkClient.im().v1().message().create(
                    CreateMessageReq.newBuilder()
                            .receiveIdType("chat_id")
                            .createMessageReqBody(CreateMessageReqBody.newBuilder()
                                    .receiveId(message.getSessionId())
                                    .msgType("text")
                                    .content(content.toString())
                                    .build())
                            .build()
            );
            if (!resp.success()) {
                log.error("Failed to send message, code={} msg={} reqId={}",
                        resp.getCode(), resp.getMsg(), resp.getRequestId());
            }
        } catch (Exception e) {
            log.error("Exception while sending message via Feishu API: {}", e.getMessage(), e);
        }
    }
}

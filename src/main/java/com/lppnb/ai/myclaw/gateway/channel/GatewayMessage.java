package com.lppnb.ai.myclaw.gateway.channel;

import lombok.Builder;
import lombok.Data;

import java.util.function.Consumer;

/**
 * 统一入站/出站消息模型，屏蔽各平台差异。
 */
@Data
@Builder
public class GatewayMessage {

    /** 平台标识，如 "feishu"、"wechat" */
    private String platform;

    /** 会话 ID，用于回复时定位目标会话 */
    private String sessionId;

    /** 发送者 ID（平台用户唯一标识） */
    private String senderId;

    /** 消息文本内容 */
    private String content;

    /** 原始平台消息对象，可空 */
    private Object rawPayload;

    /**
     * 模型每次 think 产生文本时的实时回调，可为 null。
     * 用于将中间思考内容实时推送至客户端（如飞书）。
     */
    private Consumer<String> onThought;
}

package com.lppnb.ai.myclaw.gateway.channel;

import lombok.Builder;
import lombok.Data;

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
}

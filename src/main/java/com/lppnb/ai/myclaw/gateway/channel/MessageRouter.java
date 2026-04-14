package com.lppnb.ai.myclaw.gateway.channel;

/**
 * 消息路由接口，将入站 GatewayMessage 交由 AI Agent 处理并返回回复内容。
 */
public interface MessageRouter {

    /**
     * 路由消息至 Agent 处理。
     *
     * @param message 入站统一消息
     * @return Agent 处理后的回复文本
     */
    String route(GatewayMessage message);
}

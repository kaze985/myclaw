package com.lppnb.ai.myclaw.gateway.channel;

import com.lppnb.ai.myclaw.agent.app.MyClaw;
import com.lppnb.ai.myclaw.agent.model.AgentState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于 MyClaw Agent 的默认消息路由实现。
 * 每次路由前重置 Agent 状态，以支持无状态的多轮独立对话。
 */
@Slf4j
@RequiredArgsConstructor
public class AgentMessageRouter implements MessageRouter {

    private final MyClaw agent;

    @Override
    public String route(GatewayMessage message) {
        log.info("Routing message from platform={} sender={}", message.getPlatform(), message.getSenderId());
        try {
            resetAgent();
            String reply = agent.run(message.getContent());
            log.info("Agent replied successfully for sender={}", message.getSenderId());
            return reply;
        } catch (Exception e) {
            log.error("Agent failed to process message from sender={}: {}", message.getSenderId(), e.getMessage(), e);
            return "抱歉，处理您的消息时发生错误：" + e.getMessage();
        }
    }

    /** 重置 Agent 状态，清空上下文，确保每次对话独立执行 */
    private synchronized void resetAgent() {
        agent.setState(AgentState.IDLE);
        agent.getContextMessages().clear();
        agent.setCurrentStep(0);
        agent.getReply().clear();
    }
}

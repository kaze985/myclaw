package com.lppnb.ai.myclaw.gateway.channel;

import com.lppnb.ai.myclaw.agent.app.MyClaw;
import com.lppnb.ai.myclaw.agent.model.AgentState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

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
            resetAgent(message.getOnThought());
            String agentLoopResults = agent.run(message.getContent());
            log.info("Agent replied successfully for sender={}", message.getSenderId());
            return agentLoopResults;
        } catch (Exception e) {
            log.error("Agent failed to process message from sender={}: {}", message.getSenderId(), e.getMessage(), e);
            return "抱歉，处理您的消息时发生错误：" + e.getMessage();
        } finally {
            agent.setOnThought(null);
        }
    }

    /** 重置 Agent 状态，清空上下文，确保每次对话独立执行，并设置本次思考回调 */
    private synchronized void resetAgent(Consumer<String> onThought) {
        agent.setState(AgentState.IDLE);
        agent.getContextMessages().clear();
        agent.setCurrentStep(0);
        agent.setOnThought(onThought);
    }
}

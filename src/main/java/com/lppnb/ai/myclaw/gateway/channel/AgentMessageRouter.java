package com.lppnb.ai.myclaw.gateway.channel;

import com.lppnb.ai.myclaw.agent.app.MyClaw;
import com.lppnb.ai.myclaw.agent.model.AgentState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * 基于 MyClaw Agent 的默认消息路由实现。
 * 保留上下文历史消息，支持多轮连续对话。
 */
@Slf4j
@RequiredArgsConstructor
public class AgentMessageRouter implements MessageRouter {

    private final MyClaw agent;

    private static final String CMD_NEW = "/new";

    @Override
    public synchronized String route(GatewayMessage message) {
        log.info("Routing message from platform={} sender={}", message.getPlatform(), message.getSenderId());
        String content = message.getContent();
        if (CMD_NEW.equalsIgnoreCase(content.trim())) {
            return handleNewCommand();
        }
        try {
            prepareAgent(message.getOnThought(), message.getOnToken());
            String runningResult = agent.run(content);
            log.info("Agent replied successfully for sender={}", message.getSenderId());
            return runningResult;
        } catch (Exception e) {
            log.error("Agent failed to process message from sender={}: {}", message.getSenderId(), e.getMessage(), e);
            return "抱歉，处理您的消息时发生错误：" + e.getMessage();
        } finally {
            agent.setOnThought(null);
            agent.setOnToken(null);
        }
    }

    /** 处理 /new 命令：完全重置 Agent 状态并清空上下文，开启新一轮独立对话。 */
    private synchronized String handleNewCommand() {
        log.info("Received /new command, resetting agent state and clearing context.");
        agent.setState(AgentState.IDLE);
        agent.getContextMessages().clear();
        agent.setCurrentStep(0);
        agent.setOnThought(null);
        return "清空上下文成功！";
    }

    /**
     * 准备 Agent 执行下一轮对话：重置运行状态与步数计数器，但保留上下文消息以支持连续多轮对话。
     */
    private synchronized void prepareAgent(Consumer<String> onThought, Consumer<String> onToken) {
        agent.setState(AgentState.IDLE);
        agent.setCurrentStep(0);
        agent.setOnThought(onThought);
        agent.setOnToken(onToken);
    }
}

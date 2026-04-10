package com.lppnb.ai.myclaw.agent.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import com.lppnb.ai.myclaw.agent.model.AgentState;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author kaze
 * @date 2026/4/10 10:29
 */
@Data
@Slf4j
public abstract class BaseAgent {
    /**
     * 智能体名称
     */
    private String name;
    /**
     * 智能体描述
     */
    private String description;
    /**
     * 系统提示词
     */
    private String systemPrompt;
    /**
     * 下一步提示词
     */
    private String nextStepPrompt;
    /**
     * 智能体状态
     */
    private AgentState state = AgentState.IDLE;
    /**
     * 上下文消息列表
     */
    private List<Message> contextMessages = new ArrayList<>();
    /**
     * 当前执行步数
     */
    private int currentStep = 0;
    /**
     * 最大执行步数
     */
    private int maxSteps = 10;
    /**
     * 聊天客户端
     */
    private ChatClient chatClient;


    /**
     * 运行智能体
     */
    public String run(String prompt) {
        // 1.校验
        if (StringUtils.isBlank(prompt)) {
            log.warn("Prompt is empty, skipping execution");
            return "Prompt is empty";
        }
        if (state != AgentState.IDLE) {
            log.warn("Agent is already running, skipping execution");
            return "Agent is already running";
        }
        // 2.更新状态和上下文
        state = AgentState.RUNNING;
        contextMessages.add(new UserMessage(prompt));
        // 3.agent loop
        List<String> results = new ArrayList<>();
        while (currentStep < maxSteps && state != AgentState.FINISHED) {
            currentStep++;
            String result = step();
            results.add(result);
        }
        if (currentStep >= maxSteps && state != AgentState.FINISHED) {
            log.warn("Max steps exceeded");
            state = AgentState.FINISHED;
        }
        return String.join("\n", results);
    }

    /**
     * 单步执行
     */
    protected abstract String step();
}

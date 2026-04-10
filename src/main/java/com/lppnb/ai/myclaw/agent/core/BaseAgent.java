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
        log.info("Agent [{}] staring run with prompt: {}", name, prompt);
        // 1.校验
        if (StringUtils.isBlank(prompt)) {
            log.warn("Agent [{}] Prompt is empty, skipping execution", name);
            return "Prompt is empty";
        }
        if (state != AgentState.IDLE) {
            log.warn("Agent [{}] is already running or finished, current state: {}, skipping execution", name, state);
            return "Agent is already running or finished";
        }
        // 2.更新状态和上下文
        state = AgentState.RUNNING;
        contextMessages.add(new UserMessage(prompt));
        // 3.agent loop
        List<String> results = new ArrayList<>();
        try {
            while (currentStep < maxSteps && state != AgentState.FINISHED) {
                currentStep++;
                log.info("Agent [{}] executing step {}/{}", name, currentStep, maxSteps);
                String result = step();
                log.debug("Agent [{}] step {} result: {}", name, currentStep, result);
                results.add(result);
            }
            if (currentStep >= maxSteps && state != AgentState.FINISHED) {
                log.warn("Agent [{}] Max steps ({}) exceeded", name, maxSteps);
                state = AgentState.FINISHED;
            }
            log.info("Agent [{}] run finished successfully.", name);
        } catch (Exception e) {
            state = AgentState.FINISHED;
            log.error("Agent [{}] encountered an error during execution at step {}: ", name, currentStep, e);
            results.add("Error occurred: " + e.getMessage());
        }
        return String.join("\n", results);
    }

    /**
     * 单步执行
     */
    protected abstract String step();
}

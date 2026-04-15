package com.lppnb.ai.myclaw.agent.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
     * 模型思考内容回调，每次 think 阶段产生文本时触发，可用于实时推送中间思考内容。
     * 为 null 时不触发。
     */
    private Consumer<String> onThought;




    /**
     * 运行智能体
     */
    public String run(String prompt) {
        log.info("Agent [{}] staring run with prompt: {}", name, prompt);
        // 1.校验
        if (StringUtils.isBlank(prompt)) {
            log.warn("Agent [{}] Prompt is empty, skipping execution", name);
            return "提示词不能为空";
        }
        if (state != AgentState.IDLE) {
            log.warn("Agent [{}] is already running or finished, current state: {}, skipping execution", name, state);
            return "智能体当前状态不允许执行，请稍后再试";
        }
        // 2.更新状态和上下文
        state = AgentState.RUNNING;
        contextMessages.add(new UserMessage(prompt));
        // 3.agent loop
        List<String> agentLoopResults = new ArrayList<>();
        try {
            while (currentStep < maxSteps && state != AgentState.FINISHED) {
                currentStep++;
                log.info("Agent [{}] executing step {}/{}", name, currentStep, maxSteps);
                String result = step();
                log.debug("Agent [{}] step {} result: {}", name, currentStep, result);
                agentLoopResults.add(result);
            }
            if (currentStep >= maxSteps && state != AgentState.FINISHED) {
                log.warn("Agent [{}] Max steps ({}) exceeded", name, maxSteps);
                state = AgentState.FINISHED;
            }
            log.info("Agent [{}] run finished successfully.", name);
        } catch (Exception e) {
            state = AgentState.FINISHED;
            log.error("Agent [{}] encountered an error during execution at step {}: ", name, currentStep, e);
            return "抱歉，处理您的消息时发生错误：" + e.getMessage();
        }
        return "";
    }

    /**
     * 单步执行
     */
    protected abstract String step();
}

package com.lppnb.ai.myclaw.agent.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.lppnb.ai.myclaw.agent.context.AgentContextManager;
import com.lppnb.ai.myclaw.agent.model.AgentState;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * @author kaze
 * @date 2026/4/10 14:15
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {
    private final ToolCallback[] availableTools;

    private ChatResponse toolCallChatResponse;

    private final ToolCallingManager toolCallingManager;

    private final ChatOptions chatOptions;

    /**
     * 上下文管理器（可为 null）：在每次 think 组装 prompt 前检查并压缩超窗上下文。
     */
    private AgentContextManager contextManager;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = DashScopeChatOptions.builder()
                .internalToolExecutionEnabled(false)
                .multiModel(true)
                .model("qwen3.8-max")
                .build();
    }

    @Override
    protected boolean think() {
        // 推理前检查上下文：超窗时压缩最老的对话段，防止上下文无限累积
        if (contextManager != null) {
            contextManager.prepare(getContextMessages());
        }
        if (StringUtils.isNotBlank(getNextStepPrompt())) {
            Message lastMessage = getContextMessages().isEmpty() ? null : getContextMessages().getLast();
            if (!(lastMessage instanceof UserMessage) || !getNextStepPrompt().equals(((UserMessage) lastMessage).getText())) {
                log.debug("Agent [{}] appending nextStepPrompt to context.", getName());
                getContextMessages().add(new UserMessage(getNextStepPrompt()));
            }
        }
        
        log.debug("Agent [{}] invoking chat model with {} messages.", getName(), getContextMessages().size());
        Prompt prompt = new Prompt(getContextMessages(), chatOptions);

        // 流式调用：逐段推送增量文本（onToken，Web 端真·流式打字），同时阻塞收集完整响应
        List<ChatResponse> responses;
        try {
            responses = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .stream()
                    .chatResponse()
                    .doOnNext(response -> {
                        if (getOnToken() != null && response.getResult() != null && response.getResult().getOutput() != null) {
                            String delta = response.getResult().getOutput().getText();
                            if (StringUtils.isNotBlank(delta)) {
                                getOnToken().accept(delta);
                            }
                        }
                    })
                    .collectList()
                    .block(Duration.ofMinutes(5));
        } catch (Exception e) {
            log.error("Agent [{}] chat stream failed: {}", getName(), e.getMessage(), e);
            setState(AgentState.FINISHED);
            return false;
        }

        if (responses == null || responses.isEmpty()) {
            log.warn("Agent [{}] chat response is empty (timeout or no output).", getName());
            setState(AgentState.FINISHED);
            return false;
        }

        // 合并流式分片：完整文本 + 全部工具调用，重建标准响应结构
        StringBuilder fullText = new StringBuilder();
        List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();
        for (ChatResponse response : responses) {
            if (response.getResult() == null || response.getResult().getOutput() == null) {
                continue;
            }
            AssistantMessage partial = response.getResult().getOutput();
            if (StringUtils.isNotBlank(partial.getText())) {
                fullText.append(partial.getText());
            }
            if (partial.getToolCalls() != null && !partial.getToolCalls().isEmpty()) {
                toolCalls.addAll(partial.getToolCalls());
            }
        }

        AssistantMessage assistantMessage = AssistantMessage.builder()
                .content(fullText.toString())
                .toolCalls(toolCalls)
                .build();
        this.toolCallChatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));

        String thought = assistantMessage.getText();
        if (StringUtils.isNotBlank(thought)) {
            log.info("Agent [{}] model thought:\n{}", getName(), thought);
            if (getOnThought() != null) {
                getOnThought().accept(thought);
            }
        }
        
        if (assistantMessage.hasToolCalls()) {
            log.info("Agent [{}] model requested {} tool calls.", getName(), assistantMessage.getToolCalls().size());
            assistantMessage.getToolCalls().forEach(toolCall -> 
                log.info("Agent [{}] requested tool call: {} with args: {}", getName(), toolCall.name(), toolCall.arguments()));
            return true;
        }
        
        log.info("Agent [{}] model did not request any tool calls.", getName());
        getContextMessages().add(assistantMessage);
        setState(AgentState.FINISHED);
        return false;
    }

    @Override
    protected String act() {
        log.debug("Agent [{}] executing tool calls.", getName());
        Prompt prompt = new Prompt(getContextMessages(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        
        setContextMessages(toolExecutionResult.conversationHistory());
        
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) toolExecutionResult.conversationHistory().getLast();
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> "doTerminate".equals(response.name()));
                
        if (terminateToolCalled) {
            log.info("Agent [{}] termination tool called. Ending execution.", getName());
            setState(AgentState.FINISHED);
        }
        
        return toolResponseMessage.getResponses().stream()
                .map(response -> "工具 " + response.name() + " 完成了它的任务！结果: " + response.responseData())
                .collect(Collectors.joining("\n"));
    }
}

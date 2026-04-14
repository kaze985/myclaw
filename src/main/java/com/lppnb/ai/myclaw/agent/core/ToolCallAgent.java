package com.lppnb.ai.myclaw.agent.core;

import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
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

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = DashScopeChatOptions.builder()
                .internalToolExecutionEnabled(false)
                .build();
    }

    @Override
    protected boolean think() {
        if (StringUtils.isNotBlank(getNextStepPrompt())) {
            Message lastMessage = getContextMessages().isEmpty() ? null : getContextMessages().getLast();
            if (!(lastMessage instanceof UserMessage) || !getNextStepPrompt().equals(((UserMessage) lastMessage).getText())) {
                log.debug("Agent [{}] appending nextStepPrompt to context.", getName());
                getContextMessages().add(new UserMessage(getNextStepPrompt()));
            }
        }
        
        log.debug("Agent [{}] invoking chat model with {} messages.", getName(), getContextMessages().size());
        Prompt prompt = new Prompt(getContextMessages(), chatOptions);
        ChatResponse chatResponse = getChatClient().prompt(prompt)
                .system(getSystemPrompt())
                .toolCallbacks(availableTools)
                .call()
                .chatResponse();
        this.toolCallChatResponse = chatResponse;
        
        if (chatResponse == null || chatResponse.getResult() == null) {
            log.warn("Agent [{}] chat response is null.", getName());
            setState(AgentState.FINISHED);
            return false;
        }
        
        AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
        
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

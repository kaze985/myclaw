package com.lppnb.ai.myclaw.agent.app;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import com.lppnb.ai.myclaw.agent.core.ToolCallAgent;

/**
 * @author kaze
 * @date 2026/4/10 15:57
 */
@Component
public class MyClaw extends ToolCallAgent {
    public MyClaw(ToolCallback[] allTools, ChatModel dashScopeChatModel) {
        super(allTools);
        this.setName("myclaw");
        String SYSTEM_PROMPT = """
                You are myclaw, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                After using each tool, clearly explain the execution results and suggest the next steps.
                If you want to stop the interaction at any point, use the `terminate` tool/function call.
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);

        ChatClient chatClient = ChatClient.builder(dashScopeChatModel)
                .build();
        this.setChatClient(chatClient);
    }
}

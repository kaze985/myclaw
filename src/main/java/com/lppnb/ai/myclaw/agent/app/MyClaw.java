package com.lppnb.ai.myclaw.agent.app;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import com.lppnb.ai.myclaw.agent.core.ToolCallAgent;
import com.lppnb.ai.myclaw.tool.skill.SkillCatalogGenerator;

/**
 * @author kaze
 * @date 2026/4/10 15:57
 */
@Component
public class MyClaw extends ToolCallAgent {
    private final String baseSystemPrompt;

    private final SkillCatalogGenerator skillCatalogGenerator;

    public MyClaw(ToolCallback[] allTools,
                  ChatModel dashScopeChatModel,
                  SkillCatalogGenerator skillCatalogGenerator) {
        super(allTools);
        this.skillCatalogGenerator = skillCatalogGenerator;
        this.setName("myclaw");
        String SYSTEM_PROMPT = """
                You are myclaw, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                Always respond in 中文.
                """;
        this.baseSystemPrompt = SYSTEM_PROMPT;
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

    /**
     * 动态系统提示词：基础提示词 + 最新技能目录。
     * think() 每次推理都会调用本方法，因此用户运行时新建的技能在下一轮推理立即生效。
     */
    @Override
    public String getSystemPrompt() {
        String catalog = skillCatalogGenerator.generateCatalog();
        if (catalog.isEmpty()) {
            return baseSystemPrompt;
        }
        return baseSystemPrompt + "\n\n" + catalog;
    }
}

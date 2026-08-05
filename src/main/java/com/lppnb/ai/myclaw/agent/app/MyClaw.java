package com.lppnb.ai.myclaw.agent.app;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import com.lppnb.ai.myclaw.agent.core.ToolCallAgent;
import com.lppnb.ai.myclaw.agent.context.AgentContextManager;
import com.lppnb.ai.myclaw.tool.skill.SkillCatalogGenerator;

/**
 * @author kaze
 * @date 2026/4/10 15:57
 */
@Component
public class MyClaw extends ToolCallAgent {
    private final String baseSystemPrompt;

    /**
     * 下一步指引文本：随 system prompt 每轮注入（见 {@link #getSystemPrompt()}），
     * 不写入上下文消息列表，避免长任务中重复累积（对比 ToolCallAgent 的 nextStepPrompt 机制）。
     */
    private final String nextStepText;

    private final SkillCatalogGenerator skillCatalogGenerator;

    public MyClaw(ToolCallback[] allTools,
                  ChatModel dashScopeChatModel,
                  SkillCatalogGenerator skillCatalogGenerator,
                  AgentContextManager agentContextManager) {
        super(allTools);
        this.skillCatalogGenerator = skillCatalogGenerator;
        this.setContextManager(agentContextManager);
        this.setName("myclaw");
        String SYSTEM_PROMPT = """
                You are myclaw, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                Always respond in 中文.
                """;
        this.baseSystemPrompt = SYSTEM_PROMPT;
        this.setSystemPrompt(SYSTEM_PROMPT);
        this.nextStepText = """
                Proactively select and combine the appropriate tools to complete the task, and clearly explain
                the execution results after each tool use. Use the `terminate` tool to end the interaction
                when the task is done.
                """.trim();
        // 注意：不再调用 setNextStepPrompt()——ToolCallAgent.think() 会把该字段作为
        // UserMessage 重复注入上下文（每轮工具循环一份），改为随 system prompt 注入后零累积。
        this.setMaxSteps(20);

        ChatClient chatClient = ChatClient.builder(dashScopeChatModel)
                .build();
        this.setChatClient(chatClient);
    }

    /**
     * 动态系统提示词：基础提示词 + 下一步指引 + 最新技能目录。
     * think() 每次推理都会调用本方法（system prompt 每轮重新传入），
     * 因此用户运行时新建的技能在下一轮推理立即生效，且 nextStep 指引零上下文累积。
     */
    @Override
    public String getSystemPrompt() {
        StringBuilder sb = new StringBuilder(baseSystemPrompt);
        if (StringUtils.isNotBlank(nextStepText)) {
            sb.append("\n\n").append(nextStepText);
        }
        String catalog = skillCatalogGenerator.generateCatalog();
        if (StringUtils.isNotBlank(catalog)) {
            sb.append("\n\n").append(catalog);
        }
        return sb.toString();
    }
}

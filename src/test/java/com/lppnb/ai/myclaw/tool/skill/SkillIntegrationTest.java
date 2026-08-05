package com.lppnb.ai.myclaw.tool.skill;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 集成验证：确认 loadSkill 工具已注册进 Agent 工具列表，
 * 技能目录包含内置 skill-creator，且 MyClaw 系统提示词携带目录。
 * （不依赖真实大模型 API 与飞书连接）
 */
@SpringBootTest(properties = {
        "gateway.feishu.enabled=false",
        "spring.ai.dashscope.api-key=sk-test-not-a-real-key",
        "tools.tavily.api-key=tvly-test-not-a-real-key"
})
class SkillIntegrationTest {

    @Autowired
    private ToolCallback[] allTools;

    @Autowired
    private SkillCatalogGenerator skillCatalogGenerator;

    @Autowired
    private LoadSkillTool loadSkillTool;

    @Autowired
    private com.lppnb.ai.myclaw.agent.app.MyClaw myClaw;

    @Test
    void loadSkillIsRegisteredAsTool() {
        assertTrue(Arrays.stream(allTools)
                        .anyMatch(t -> "loadSkill".equals(t.getToolDefinition().name())),
                "loadSkill 应注册到 Agent 工具列表");
    }

    @Test
    void catalogContainsBuiltinSkillCreator() {
        String catalog = skillCatalogGenerator.generateCatalog();
        assertTrue(catalog.contains("skill-creator"), "目录应包含内置 skill-creator");
        assertTrue(catalog.contains("loadSkill"), "目录使用指引应提及 loadSkill");
    }

    @Test
    void myClawSystemPromptCarriesCatalogAndNextStep() {
        String systemPrompt = myClaw.getSystemPrompt();
        assertTrue(systemPrompt.contains("可用技能目录"), "MyClaw 系统提示词应包含技能目录");
        assertTrue(systemPrompt.contains("skill-creator"), "系统提示词应包含 skill-creator 条目");
        assertTrue(systemPrompt.contains("Proactively select and combine"),
                "下一步指引应随系统提示词注入（而非 UserMessage 累积）");
        // nextStepPrompt 字段保持为空，ToolCallAgent.think() 因此不会把它重复注入上下文消息
        assertTrue(myClaw.getNextStepPrompt() == null || myClaw.getNextStepPrompt().isEmpty(),
                "nextStepPrompt 字段应为空，避免 UserMessage 重复注入");
    }

    @Test
    void loadSkillBeanLoadsBuiltinCreator() {
        String result = loadSkillTool.loadSkill("skill-creator");
        assertTrue(result.contains("技能创建助手"), "集成上下文中的 loadSkill 应能加载内置技能正文");
    }
}

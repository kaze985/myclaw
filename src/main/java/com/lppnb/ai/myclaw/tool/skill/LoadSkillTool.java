package com.lppnb.ai.myclaw.tool.skill;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.lppnb.ai.myclaw.agent.context.AgentContextProperties;
import com.lppnb.ai.myclaw.tool.ToolResultTruncator;

import lombok.extern.slf4j.Slf4j;

/**
 * 按需加载技能工具：根据技能 name 返回技能正文（不含 frontmatter），
 * 正文作为工具结果进入对话上下文（渐进式暴露，不常驻系统提示词）。
 * 超长正文按 {@link AgentContextProperties#getMaxSkillBodyChars()} 截断，
 * 避免单条技能正文占用过多上下文预算。
 */
@Slf4j
@Component
public class LoadSkillTool {

    private final SkillRepository skillRepository;

    private final AgentContextProperties properties;

    public LoadSkillTool(SkillRepository skillRepository, AgentContextProperties properties) {
        this.skillRepository = skillRepository;
        this.properties = properties;
    }

    @Tool(description = """
            Load the detailed instructions of a skill by its name. Skills are reusable prompt
            packages listed in the skill catalog (see the system prompt). Call this tool before
            executing a skill to obtain its step-by-step instructions.
            """)
    public String loadSkill(
            @ToolParam(description = "The name of the skill to load, e.g. weekly-report") String name) {
        if (name == null || name.isBlank()) {
            log.warn("loadSkill 收到空的技能名称");
            return "技能不存在：" + name;
        }
        String normalized = name.trim();
        String body = skillRepository.load(normalized);
        if (body == null) {
            log.warn("loadSkill 未找到技能：{}", normalized);
            return "技能不存在：" + normalized;
        }
        log.info("loadSkill 加载技能成功：{}（正文 {} 字符）", normalized, body.length());
        return ToolResultTruncator.truncate(body, properties.getMaxSkillBodyChars());
    }
}

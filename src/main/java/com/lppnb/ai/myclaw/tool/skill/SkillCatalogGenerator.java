package com.lppnb.ai.myclaw.tool.skill;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.lppnb.ai.myclaw.agent.context.AgentContextProperties;
import com.lppnb.ai.myclaw.tool.skill.SkillRepository.SkillInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * 技能目录生成器：将当前全部可用技能（内置 + 全局 + 项目用户）渲染为一行一条的目录文本，
 * 供注入 Agent 系统提示词，让模型感知可用技能并在需要时调用 loadSkill 加载正文。
 * 目录条数超过 {@link AgentContextProperties#getMaxCatalogEntries()} 时折叠为计数提示，
 * 防止技能数量增长导致 system prompt 膨胀。
 */
@Slf4j
@Component
public class SkillCatalogGenerator {

    private final SkillRepository skillRepository;

    private final AgentContextProperties properties;

    public SkillCatalogGenerator(SkillRepository skillRepository, AgentContextProperties properties) {
        this.skillRepository = skillRepository;
        this.properties = properties;
    }

    /**
     * 生成技能目录文本（每次调用重新扫描，反映最新技能集合）。
     *
     * @return 目录文本；无可用技能时返回空字符串
     */
    public String generateCatalog() {
        List<SkillInfo> skills = skillRepository.listCatalog();
        if (skills.isEmpty()) {
            log.debug("技能目录为空，无可用技能");
            return "";
        }
        int limit = properties.getMaxCatalogEntries();
        List<SkillInfo> shown = skills.size() > limit ? skills.subList(0, limit) : skills;
        log.debug("技能目录生成：共 {} 个技能{}", skills.size(),
                skills.size() > limit ? "，仅展示前 " + limit + " 个" : "");
        String lines = shown.stream()
                .map(s -> "- " + s.name() + ": " + s.description())
                .collect(Collectors.joining("\n"));
        if (skills.size() > limit) {
            lines += "\n- …（共 " + skills.size() + " 个技能，仅展示前 " + limit + " 个，如需其他技能请询问）";
        }
        return """
                可用技能目录（如需使用某个技能完成任务，请先调用 loadSkill 加载其正文指令）：
                %s""".formatted(lines);
    }
}

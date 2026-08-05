package com.lppnb.ai.myclaw.tool.skill;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.lppnb.ai.myclaw.agent.context.AgentContextProperties;
import com.lppnb.ai.myclaw.tool.skill.SkillRepository.SkillInfo;

class SkillCatalogGeneratorTest {

    private List<SkillInfo> skillsOfSize(int n) {
        return IntStream.range(0, n)
                .mapToObj(i -> new SkillInfo("skill-" + i, "描述 " + i))
                .toList();
    }

    @Test
    void catalogBelowLimitShowsAll() {
        SkillRepository repo = mock(SkillRepository.class);
        when(repo.listCatalog()).thenReturn(skillsOfSize(10));
        SkillCatalogGenerator generator = new SkillCatalogGenerator(repo, new AgentContextProperties());

        String catalog = generator.generateCatalog();
        assertTrue(catalog.contains("- skill-0:"));
        assertTrue(catalog.contains("- skill-9:"));
        assertFalse(catalog.contains("仅展示前"), "未超限不应出现折叠提示");
    }

    @Test
    void catalogAboveLimitIsFoldedWithCount() {
        SkillRepository repo = mock(SkillRepository.class);
        when(repo.listCatalog()).thenReturn(skillsOfSize(60));
        SkillCatalogGenerator generator = new SkillCatalogGenerator(repo, new AgentContextProperties());

        String catalog = generator.generateCatalog();
        assertTrue(catalog.contains("- skill-0:"));
        assertTrue(catalog.contains("- skill-49:"), "应展示前 50 个");
        assertFalse(catalog.contains("- skill-50:"), "超出部分不应逐条展示");
        assertTrue(catalog.contains("共 60 个技能，仅展示前 50 个"), "应包含折叠计数提示");
    }

    @Test
    void emptyCatalogReturnsEmpty() {
        SkillRepository repo = mock(SkillRepository.class);
        when(repo.listCatalog()).thenReturn(List.of());
        SkillCatalogGenerator generator = new SkillCatalogGenerator(repo, new AgentContextProperties());
        assertTrue(generator.generateCatalog().isEmpty());
    }
}

package com.lppnb.ai.myclaw.tool.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.lppnb.ai.myclaw.tool.skill.SkillFileParser.Parsed;
import com.lppnb.ai.myclaw.tool.skill.SkillRepository.SkillInfo;

class SkillRepositoryTest {

    @TempDir
    Path tempDir;

    private String userSkillsDir;
    private String globalSkillsDir;

    @BeforeEach
    void setUp() {
        userSkillsDir = tempDir.resolve("user-skills").toString();
        globalSkillsDir = tempDir.resolve("global-skills").toString();
    }

    private SkillRepository newRepository() {
        return new SkillRepository(userSkillsDir, globalSkillsDir);
    }

    private void writeUserSkill(String fileName, String content) throws IOException {
        Files.createDirectories(Path.of(userSkillsDir));
        Files.writeString(Path.of(userSkillsDir, fileName), content);
    }

    private void writeNestedSkill(String rootDir, String skillName, String content) throws IOException {
        Path dir = Path.of(rootDir, skillName);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("SKILL.md"), content);
    }

    private void writeGlobalSkill(String skillName, String content) throws IOException {
        writeNestedSkill(globalSkillsDir, skillName, content);
    }

    // ---- SkillFileParser ----

    @Test
    void parseValidFrontmatter() {
        String content = """
                ---
                name: weekly-report
                description: 生成周报
                ---
                你是周报专家。
                """;
        Parsed parsed = SkillFileParser.parse(content);
        assertNotNull(parsed);
        assertEquals("weekly-report", parsed.name());
        assertEquals("生成周报", parsed.description());
        assertEquals("你是周报专家。", parsed.body());
    }

    @Test
    void parseMissingNameReturnsNull() {
        String content = """
                ---
                description: 没有 name
                ---
                正文
                """;
        assertNull(SkillFileParser.parse(content));
    }

    @Test
    void parseNoFrontmatterReturnsNull() {
        assertNull(SkillFileParser.parse("plain markdown without frontmatter"));
        assertNull(SkillFileParser.parse(""));
        assertNull(SkillFileParser.parse(null));
    }

    @Test
    void parseUnclosedFrontmatterReturnsNull() {
        String content = """
                ---
                name: broken
                """;
        assertNull(SkillFileParser.parse(content));
    }

    // ---- SkillRepository ----

    @Test
    void catalogContainsBuiltinSkillCreator() {
        SkillRepository repo = newRepository();
        repo.init();
        List<SkillInfo> catalog = repo.listCatalog();
        assertTrue(catalog.stream().anyMatch(s -> "skill-creator".equals(s.name())),
                "classpath 内置 skill-creator（嵌套格式）应出现在目录中");
    }

    @Test
    void userSkillOverridesBuiltinSameName() throws IOException {
        writeNestedSkill(userSkillsDir, "skill-creator", """
                ---
                name: skill-creator
                description: 用户自定义版本描述
                ---
                用户自定义正文
                """);
        SkillRepository repo = newRepository();
        repo.init();

        List<SkillInfo> catalog = repo.listCatalog();
        SkillInfo creator = catalog.stream()
                .filter(s -> "skill-creator".equals(s.name()))
                .findFirst()
                .orElseThrow();
        assertEquals("用户自定义版本描述", creator.description(), "同名技能应以项目用户目录为准");

        String body = repo.load("skill-creator");
        assertEquals("用户自定义正文", body);
    }

    @Test
    void nestedFormatSkillIsScannedAndLoaded() throws IOException {
        writeNestedSkill(userSkillsDir, "grill-me", """
                ---
                name: grill-me
                description: 磨砺计划
                ---
                你是无情采访者。
                """);
        SkillRepository repo = newRepository();
        repo.init();

        List<SkillInfo> catalog = repo.listCatalog();
        assertTrue(catalog.stream().anyMatch(s -> "grill-me".equals(s.name())),
                "嵌套格式 skills/grill-me/SKILL.md 应被扫描到");
        assertEquals("你是无情采访者。", repo.load("grill-me"));
    }

    @Test
    void globalSkillsDirIsScanned() throws IOException {
        writeGlobalSkill("meeting-minutes", """
                ---
                name: meeting-minutes
                description: 会议纪要
                ---
                步骤一：记录结论。
                """);
        SkillRepository repo = newRepository();
        repo.init();

        List<SkillInfo> catalog = repo.listCatalog();
        assertTrue(catalog.stream().anyMatch(s -> "meeting-minutes".equals(s.name())),
                "user.home/.agents/skills 下的全局技能应被扫描到");
        assertEquals("步骤一：记录结论。", repo.load("meeting-minutes"));
    }

    @Test
    void priorityIsUserOverGlobalOverBuiltin() throws IOException {
        // 内置：skill-creator
        writeGlobalSkill("skill-creator", """
                ---
                name: skill-creator
                description: 全局安装版本
                ---
                全局正文
                """);
        writeNestedSkill(userSkillsDir, "skill-creator", """
                ---
                name: skill-creator
                description: 项目用户版本
                ---
                项目正文
                """);
        SkillRepository repo = newRepository();
        repo.init();

        assertEquals("项目正文", repo.load("skill-creator"), "项目用户目录应覆盖全局目录与内置");
        SkillInfo info = repo.listCatalog().stream()
                .filter(s -> "skill-creator".equals(s.name()))
                .findFirst().orElseThrow();
        assertEquals("项目用户版本", info.description());

        // 移除项目用户版本后，全局版本生效
        Files.delete(Path.of(userSkillsDir, "skill-creator", "SKILL.md"));
        assertEquals("全局正文", repo.load("skill-creator"), "无项目版本时全局版本生效");
    }

    @Test
    void loadUserSkillBodyExcludesFrontmatter() throws IOException {
        writeUserSkill("meeting-minutes.md", """
                ---
                name: meeting-minutes
                description: 会议纪要
                ---
                步骤一：记录结论。
                """);
        SkillRepository repo = newRepository();
        repo.init();

        String body = repo.load("meeting-minutes");
        assertEquals("步骤一：记录结论。", body);
        assertTrue(!body.contains("description"), "正文不应包含 frontmatter");
    }

    @Test
    void loadUnknownSkillReturnsNull() {
        SkillRepository repo = newRepository();
        repo.init();
        assertNull(repo.load("no-such-skill"));
        assertNull(repo.load(null));
        assertNull(repo.load("  "));
    }

    @Test
    void loadRejectsPathTraversalName() throws IOException {
        // 在临时目录外放一个敏感文件，验证 name 白名单拒绝路径穿越
        Path outside = tempDir.getParent().resolve("secret.txt");
        Files.writeString(outside, "top secret");
        SkillRepository repo = newRepository();
        repo.init();
        assertNull(repo.load(".." + "/" + outside.getFileName()), "含 ../ 的 name 应被拒绝");
        assertNull(repo.load("../secret"), "相对穿越应被拒绝");
        assertNull(repo.load("SECRET"), "大写字母不在 kebab-case 白名单内");
        assertNull(repo.load("a b"), "空格应被拒绝");
    }

    @Test
    void invalidUserFileIsSkippedInCatalog() throws IOException {
        writeNestedSkill(userSkillsDir, "broken", "no frontmatter here");
        writeNestedSkill(userSkillsDir, "valid", """
                ---
                name: valid
                description: 有效技能
                ---
                正文
                """);
        SkillRepository repo = newRepository();
        repo.init();

        List<SkillInfo> catalog = repo.listCatalog();
        assertTrue(catalog.stream().noneMatch(s -> "broken".equals(s.name())),
                "无 frontmatter 的文件应被跳过");
        assertTrue(catalog.stream().anyMatch(s -> "valid".equals(s.name())));
    }

    @Test
    void ensureUserSkillsDirCreatesDirectory() {
        SkillRepository repo = new SkillRepository(userSkillsDir, globalSkillsDir);
        repo.ensureUserSkillsDir();
        assertTrue(Files.isDirectory(Path.of(userSkillsDir)), "用户技能目录应被创建");
    }

    // ---- LoadSkillTool ----

    @Test
    void loadSkillToolReturnsBodyForExistingSkill() {
        SkillRepository repo = newRepository();
        repo.init();
        LoadSkillTool tool = new LoadSkillTool(repo);
        String result = tool.loadSkill("skill-creator");
        assertNotNull(result);
        assertTrue(result.contains("技能创建助手"), "应返回内置 skill-creator 正文");
    }

    @Test
    void loadSkillToolReturnsErrorForUnknownSkill() {
        SkillRepository repo = newRepository();
        repo.init();
        LoadSkillTool tool = new LoadSkillTool(repo);
        String result = tool.loadSkill("no-such-skill");
        assertEquals("技能不存在：no-such-skill", result);
    }
}

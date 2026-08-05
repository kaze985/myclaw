package com.lppnb.ai.myclaw.tool.skill;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.lppnb.ai.myclaw.tool.skill.SkillFileParser.Parsed;

import cn.hutool.core.io.FileUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 技能仓库：统一管理三个来源的 Skill 文件（同名时按优先级覆盖，高者生效）——
 * <ol>
 *   <li>项目用户技能目录 {@code user.dir/skills}（运行时通过对话创建，优先级最高）；</li>
 *   <li>全局安装技能目录 {@code user.home/.agents/skills}（如 npx skills add 安装的 skill）；</li>
 *   <li>classpath {@code skills/}（随应用发布的内置技能，如 skill-creator，优先级最低）。</li>
 * </ol>
 * <p>目录格式统一为 {@code <根>/<name>/SKILL.md}（每个技能一个子目录）；
 * 同时兼容旧的扁平格式 {@code <根>/<name>.md}（读侧兼容，不再新产生）。</p>
 */
@Slf4j
@Component
public class SkillRepository {

    private static final String[] CLASS_SKILLS_PATTERNS = {
            "classpath*:skills/*/SKILL.md",   // 标准嵌套格式
            "classpath*:skills/*.md"          // 兼容旧扁平格式
    };
    private static final String CLASS_SKILLS_PREFIX = "classpath:skills/";

    /** 技能定义文件名（嵌套格式）。 */
    private static final String SKILL_FILE_NAME = "SKILL.md";

    /** name 白名单：kebab-case（小写字母、数字、连字符），防止路径穿越。 */
    private static final java.util.regex.Pattern NAME_PATTERN =
            java.util.regex.Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private final String userSkillsDir;
    private final String globalSkillsDir;
    private final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    /** 技能目录条目：name + description，供目录注入与展示。 */
    public record SkillInfo(String name, String description) {
    }

    /** 默认：项目用户目录 {@code user.dir/skills}，全局目录 {@code user.home/.agents/skills}。 */
    public SkillRepository() {
        this(System.getProperty("user.dir") + File.separator + "skills",
                System.getProperty("user.home") + File.separator + ".agents" + File.separator + "skills");
    }

    /** 供测试注入自定义目录。 */
    SkillRepository(String userSkillsDir, String globalSkillsDir) {
        this.userSkillsDir = userSkillsDir;
        this.globalSkillsDir = globalSkillsDir;
    }

    /** 初始化时确保项目用户技能目录存在。 */
    @PostConstruct
    public void init() {
        ensureUserSkillsDir();
        log.info("技能仓库初始化完成：项目用户目录={}，全局目录={}", userSkillsDir, globalSkillsDir);
    }

    /** 确保项目用户技能目录存在，不存在则创建。 */
    public void ensureUserSkillsDir() {
        FileUtil.mkdir(userSkillsDir);
        if (!new File(userSkillsDir).isDirectory()) {
            log.warn("Failed to create user skills directory: {}", userSkillsDir);
        }
    }

    /**
     * 返回全部可用技能的目录条目（三个来源，同名以高优先级来源为准），按 name 排序。
     */
    public List<SkillInfo> listCatalog() {
        Map<String, SkillInfo> byName = new LinkedHashMap<>();
        // 优先级从低到高依次扫描，后者覆盖前者同名项
        List<Parsed> classpathSkills = scanClasspathSkills();
        List<Parsed> globalSkills = scanDirSkills(globalSkillsDir);
        List<Parsed> userSkills = scanDirSkills(userSkillsDir);
        for (Parsed parsed : classpathSkills) {
            byName.putIfAbsent(parsed.name(), new SkillInfo(parsed.name(), parsed.description()));
        }
        for (Parsed parsed : globalSkills) {
            byName.put(parsed.name(), new SkillInfo(parsed.name(), parsed.description()));
        }
        for (Parsed parsed : userSkills) {
            byName.put(parsed.name(), new SkillInfo(parsed.name(), parsed.description()));
        }
        log.debug("技能目录统计：内置 {} 个，全局 {} 个，项目用户 {} 个，生效 {} 个",
                classpathSkills.size(), globalSkills.size(), userSkills.size(), byName.size());
        return byName.values().stream()
                .sorted(Comparator.comparing(SkillInfo::name))
                .toList();
    }

    /**
     * 加载技能正文（不含 frontmatter）。按来源优先级：项目用户目录 &gt; 全局安装目录 &gt; 内置资源。
     * 每个来源优先查找嵌套格式 {@code <name>/SKILL.md}，其次兼容扁平格式 {@code <name>.md}。
     *
     * @param name 技能 name（kebab-case）
     * @return 技能正文；技能不存在或解析失败时返回 {@code null}
     */
    public String load(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.trim();
        if (!NAME_PATTERN.matcher(normalized).matches()) {
            log.warn("Rejected invalid skill name (path traversal attempt?): {}", normalized);
            return null;
        }

        String fromUserDir = loadFromDir("项目用户目录", userSkillsDir, normalized);
        if (fromUserDir != null) {
            return fromUserDir;
        }
        String fromGlobalDir = loadFromDir("全局安装目录", globalSkillsDir, normalized);
        if (fromGlobalDir != null) {
            return fromGlobalDir;
        }
        String fromClasspath = loadFromClasspath(normalized);
        if (fromClasspath != null) {
            return fromClasspath;
        }
        log.debug("技能 [{}] 在三个来源中均未找到", normalized);
        return null;
    }

    private String loadFromDir(String source, String rootDir, String name) {
        File nested = new File(new File(rootDir, name), SKILL_FILE_NAME);
        if (nested.isFile()) {
            Parsed parsed = SkillFileParser.parse(FileUtil.readUtf8String(nested));
            if (parsed != null) {
                log.info("技能 [{}] 加载命中，来源：{}（嵌套格式）", name, source);
                return parsed.body();
            }
            log.warn("Invalid skill file, fallback skipped: {}", nested.getAbsolutePath());
            return null;
        }
        File flat = new File(rootDir, name + ".md");
        if (flat.isFile()) {
            Parsed parsed = SkillFileParser.parse(FileUtil.readUtf8String(flat));
            if (parsed != null) {
                log.info("技能 [{}] 加载命中，来源：{}（旧扁平格式）", name, source);
                return parsed.body();
            }
            log.warn("Invalid skill file, fallback skipped: {}", flat.getAbsolutePath());
            return null;
        }
        return null;
    }

    private String loadFromClasspath(String name) {
        for (String suffix : new String[]{name + "/" + SKILL_FILE_NAME, name + ".md"}) {
            Resource resource = resourceResolver.getResource(CLASS_SKILLS_PREFIX + suffix);
            if (resource.exists()) {
                try {
                    Parsed parsed = SkillFileParser.parse(resource.getContentAsString(StandardCharsets.UTF_8));
                    if (parsed != null) {
                        log.info("技能 [{}] 加载命中，来源：内置（{}）", name, suffix);
                        return parsed.body();
                    }
                } catch (IOException e) {
                    log.warn("Failed to read classpath skill: {}", suffix, e);
                }
            }
        }
        return null;
    }

    private List<Parsed> scanClasspathSkills() {
        List<Parsed> result = new ArrayList<>();
        try {
            for (String pattern : CLASS_SKILLS_PATTERNS) {
                Resource[] resources = resourceResolver.getResources(pattern);
                for (Resource resource : resources) {
                    try {
                        Parsed parsed = SkillFileParser.parse(resource.getContentAsString(StandardCharsets.UTF_8));
                        if (parsed != null) {
                            result.add(parsed);
                        } else {
                            log.warn("Skip invalid classpath skill file: {}", resource.getFilename());
                        }
                    } catch (IOException e) {
                        log.warn("Failed to read classpath skill resource: {}", resource.getFilename(), e);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to scan classpath skills with patterns {}", Arrays.toString(CLASS_SKILLS_PATTERNS), e);
        }
        return result;
    }

    private List<Parsed> scanDirSkills(String rootDir) {
        List<Parsed> result = new ArrayList<>();
        File root = new File(rootDir);
        if (!root.isDirectory()) {
            return result;
        }
        collectSkillFiles(root, result);
        return result;
    }

    /**
     * 只收集技能定义文件：嵌套标准 {@code <root>/<name>/SKILL.md}（一级子目录）
     * 与旧扁平格式 {@code <root>/<name>.md}（根下直接文件）。
     * 不深递归，避免把技能目录内的辅助资源（如 references/*.md）误当技能定义。
     */
    private void collectSkillFiles(File root, List<Parsed> result) {
        File[] entries = root.listFiles();
        if (entries == null) {
            return;
        }
        for (File entry : entries) {
            if (entry.isFile()) {
                // 兼容旧扁平格式：<root>/<name>.md
                if (entry.getName().toLowerCase().endsWith(".md")) {
                    addIfValid(entry, result);
                }
            } else if (entry.isDirectory()) {
                // 标准嵌套格式：<root>/<name>/SKILL.md（一级子目录，不深递归）
                File skillFile = findSkillDefinition(entry);
                if (skillFile != null) {
                    addIfValid(skillFile, result);
                }
            }
        }
    }

    /** 在技能目录内查找定义文件 SKILL.md（大小写不敏感兜底，兼容 Linux 上小写命名）。 */
    private File findSkillDefinition(File dir) {
        File exact = new File(dir, SKILL_FILE_NAME);
        if (exact.isFile()) {
            return exact;
        }
        File[] candidates = dir.listFiles((d, n) -> n.equalsIgnoreCase(SKILL_FILE_NAME));
        if (candidates != null && candidates.length > 0) {
            return candidates[0];
        }
        return null;
    }

    private void addIfValid(File file, List<Parsed> result) {
        Parsed parsed = SkillFileParser.parse(FileUtil.readUtf8String(file));
        if (parsed != null) {
            result.add(parsed);
        } else {
            log.warn("Skip invalid skill file: {}", file.getAbsolutePath());
        }
    }
}

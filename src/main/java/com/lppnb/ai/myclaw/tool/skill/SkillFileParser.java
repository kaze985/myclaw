package com.lppnb.ai.myclaw.tool.skill;

import java.util.Arrays;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

/**
 * Skill 文件 frontmatter 解析器：将带 `---` YAML frontmatter 的 Markdown 技能文档
 * 解析为 {@code name} / {@code description} / 正文三部分。
 * 解析失败或缺少必填 {@code name} 字段时返回 {@code null}（调用方跳过该文件）。
 */
public final class SkillFileParser {

    private static final String DELIMITER = "---";

    private SkillFileParser() {
    }

    /**
     * 解析结果：技能标识、描述、正文（不含 frontmatter）。
     */
    public record Parsed(String name, String description, String body) {
    }

    /**
     * 解析技能文件内容。
     *
     * @param content 文件原始文本（允许以 BOM 或空行开头）
     * @return 解析结果；无 frontmatter、frontmatter 未闭合或缺少 {@code name} 时返回 {@code null}
     */
    public static Parsed parse(String content) {
        if (content == null) {
            return null;
        }
        String text = stripBom(content);
        String[] lines = text.split("\n", -1);

        int start = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            if (DELIMITER.equals(line)) {
                start = i;
            }
            break; // 第一个非空行若不是 ---，则无 frontmatter
        }
        if (start < 0) {
            return null;
        }

        int end = -1;
        for (int i = start + 1; i < lines.length; i++) {
            if (DELIMITER.equals(lines[i].trim())) {
                end = i;
                break;
            }
        }
        if (end < 0) {
            return null; // frontmatter 未闭合
        }

        String yamlBlock = String.join("\n", Arrays.copyOfRange(lines, start + 1, end));
        String body = String.join("\n", Arrays.copyOfRange(lines, end + 1, lines.length)).trim();

        Map<String, Object> fields;
        try {
            fields = new Yaml().load(yamlBlock);
        } catch (Exception e) {
            return null; // YAML 解析失败
        }
        if (fields == null) {
            return null;
        }

        Object nameValue = fields.get("name");
        if (!(nameValue instanceof String name) || name.isBlank()) {
            return null; // 缺少必填 name
        }
        Object descValue = fields.get("description");
        String description = descValue instanceof String desc ? desc : "";
        return new Parsed(name.trim(), description, body);
    }

    private static String stripBom(String s) {
        return !s.isEmpty() && s.charAt(0) == '\uFEFF' ? s.substring(1) : s;
    }
}

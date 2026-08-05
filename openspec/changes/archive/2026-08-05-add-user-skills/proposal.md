## Why

MyClaw 目前所有工具均为编译期静态注册，任何新能力（如"生成周报"、"会议纪要整理"）都必须改代码、重新编译、重启服务才能生效。用户希望在不改代码的前提下，通过对话让 Agent 学会新的任务流程——即 Skill（技能）机制：一个 skill 是一段带 frontmatter 的 Markdown 提示词包，描述何时使用、如何执行，用户可自行创建并全局共享。

## What Changes

- 引入 **Skill 文件格式**：带 `name` / `description` frontmatter 的 Markdown 提示词包（Claude Skills 同款），正文为任务指令；目录格式统一为 `<目录根>/<name>/SKILL.md`（读侧兼容旧扁平 `<name>.md`）。
- 新增 **`loadSkill` 工具**：Agent 按 `name` 从三个来源加载 skill 正文（classpath 内置、`user.home/.agents/skills` 全局安装、`user.dir/skills` 项目用户，同名高优先级覆盖），正文进入对话上下文（渐进式暴露，不常驻 system prompt）；`name` 经 kebab-case 白名单校验，防路径穿越。
- 新增 **Skill 目录注入**：system prompt 常驻一份轻量目录（每个 skill 一行：`name` + `description`），让模型知道有哪些技能可用及何时该加载；目录条数超过上限（默认 50）时折叠为计数提示。
- 新增 **预置 `skill-creator` 技能**：一份普通 skill 预置在 `classpath:skills/skill-creator/SKILL.md`，正文指导 Agent 如何用现有 `sandboxedFileOpsTool` 在 `user.dir/skills/` 下按嵌套格式创建新 skill 文件（禁止 `~`/绝对路径）。用户直接描述需求，Agent 自动完成创建。
- 正文长度上限：`loadSkill` 返回正文超过上限（默认 8000 字符）时截断并注明，控制上下文预算。
- 第一版**不做**：重名检测（同名直接覆盖）、编辑、删除、禁用、内容检索（仅目录 + 按需加载）。

## Capabilities

### New Capabilities
- `user-skills`: 用户通过对话创建、共享、按需使用的 Skill 机制——skill 文件格式、三来源目录（classpath 内置 + `user.home/.agents/skills` 全局安装 + `user.dir/skills` 项目用户）、目录注入、`loadSkill` 按需加载、预置 `skill-creator` 技能。

### Modified Capabilities
<!-- 无：现有 spec 的 REQUIREMENTS 不变，仅新增能力 -->

## Impact

- 代码：新增 `tool/skill/` 包四类——`SkillRepository`（三来源扫描/加载/优先级）、`SkillFileParser`（frontmatter 解析）、`LoadSkillTool`（`loadSkill` 工具）、`SkillCatalogGenerator`（目录文本生成）；新增资源 `classpath:skills/skill-creator/SKILL.md`；修改 `ToolRegistration`（注册 `loadSkill`）、`MyClaw`（system prompt 拼接目录）、`AgentContextProperties`（新增 `maxSkillBodyChars`、`maxCatalogEntries` 配置项）。
- 文件系统：新增 `user.dir/skills/` 目录（位于现有沙盒边界内，复用 `sandboxedFileOpsTool` 写入能力）；`user.home/.agents/skills/` 作为全局安装来源被扫描（不主动创建）。
- 依赖：无新增第三方依赖——frontmatter 用 Spring Boot 自带 SnakeYAML，文件遍历/读写复用既有 hutool。
- 风险：skill 正文直接进入 LLM 上下文，全局共享 + 完全信任带来提示注入面扩大——第一版按设计接受，由终端白名单等既有管控兜底；`name` 白名单校验与正文截断/目录折叠缓解路径穿越与上下文膨胀。

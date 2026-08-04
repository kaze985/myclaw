## Why

MyClaw 目前只有 11 个编译期静态注册的工具，任何新能力（如"生成周报"、"会议纪要整理"）都必须改代码、重新编译、重启服务才能生效。用户希望在不改代码的前提下，通过对话让 Agent 学会新的任务流程——即 Skill（技能）机制：一个 skill 是一段带 frontmatter 的 Markdown 提示词包，描述何时使用、如何执行，用户可自行创建并全局共享。

## What Changes

- 引入 **Skill 文件格式**：带 `name` / `description` frontmatter 的 Markdown 提示词包（Claude Skills 同款），正文为任务指令。
- 新增 **`loadSkill` 工具**：Agent 按 `name` 从内置或用户目录加载 skill 正文，正文进入对话上下文（渐进式暴露，不常驻 system prompt）。
- 新增 **Skill 目录注入**：system prompt 常驻一份轻量目录（每个 skill 一行：`name` + `description`），让模型知道有哪些技能可用及何时该加载。
- 新增 **预置 `skill-creator` 技能**：一份普通 skill 预置在 `classpath:skills/`，正文指导 Agent 如何用现有 `sandboxedFileOpsTool` 在 `user.dir/skills/` 下创建新 skill 文件。用户直接描述需求，Agent 自动完成创建。
- 第一版**不做**：重名检测（同名直接覆盖）、编辑、删除、禁用、数量上限、内容检索（仅目录 + 按需加载）。

## Capabilities

### New Capabilities
- `user-skills`: 用户通过对话创建、共享、按需使用的 Skill 机制——skill 文件格式、双来源目录（classpath 内置 + `user.dir/skills/` 用户目录）、目录注入、`loadSkill` 按需加载、预置 `skill-creator` 技能。

### Modified Capabilities
<!-- 无：现有 spec 的 REQUIREMENTS 不变，仅新增能力 -->

## Impact

- 代码：新增 `tool/skill/` 包（`LoadSkillTool`）、`SkillCatalogGenerator`（目录扫描与 frontmatter 解析）、`classpath:skills/skill-creator.md` 资源；修改 `ToolRegistration`（注册 `loadSkill`）与 `MyClaw`（system prompt 拼接目录）。
- 文件系统：新增 `user.dir/skills/` 目录（位于现有沙盒边界内，复用 `sandboxedFileOpsTool` 写入能力）。
- 依赖：无新增第三方依赖（frontmatter 用轻量自解析，不引入 YAML 库）。
- 风险：skill 正文直接进入 LLM 上下文，全局共享 + 完全信任带来提示注入面扩大——第一版按设计接受，由终端白名单等既有管控兜底。

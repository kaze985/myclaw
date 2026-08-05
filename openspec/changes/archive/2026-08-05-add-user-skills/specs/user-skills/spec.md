## Purpose

为 MyClaw 引入 Skill（技能）机制：用户无需改代码，通过对话即可让 Agent 创建可复用的提示词包技能；技能以 Markdown + frontmatter 文件存储，Agent 通过目录感知技能存在、按需加载正文执行任务，实现能力的渐进式暴露与全局共享。

## ADDED Requirements

### Requirement: Skill 文件格式与存储

系统 SHALL 支持 Skill 文件，格式为带 YAML frontmatter 的 Markdown 文档，frontmatter 包含必填字段 `name`（技能唯一标识，kebab-case）与 `description`（技能能力说明与触发场景），frontmatter 之后为技能正文（任务指令）。

Skill 文件的目录格式 SHALL 统一为 `<目录根>/<name>/SKILL.md`（每个技能一个子目录，定义文件名为 `SKILL.md`）；同时 SHALL 兼容读取旧的扁平格式 `<目录根>/<name>.md`（读侧兼容，不再新产生）。

Skill 文件 SHALL 来自三个来源，同名时按以下优先级生效（高者覆盖低者）：
1. 项目用户技能目录 `user.dir/skills/`（通过对话创建，全局共享）；
2. 全局安装技能目录 `user.home/.agents/skills/`（如 `npx skills add` 安装的 skill）；
3. classpath `skills/`（随应用发布的内置技能，如 `skill-creator`）。

#### Scenario: 嵌套格式的技能文件被识别
- **WHEN** `user.dir/skills/weekly-report/SKILL.md` 存在，其 frontmatter 含 `name: weekly-report` 与描述文本
- **THEN** 该技能被识别为可用技能，其 `name` 与 `description` 出现在技能目录中，正文可按需加载

#### Scenario: 全局安装目录中的技能被识别
- **WHEN** `user.home/.agents/skills/grill-me/SKILL.md` 存在且 frontmatter 合法
- **THEN** `grill-me` 被识别为可用技能，出现在技能目录中且正文可按需加载

#### Scenario: 三个来源同名时的优先级
- **WHEN** 内置、全局安装目录、项目用户目录同时存在同名技能 `skill-creator`
- **THEN** 项目用户目录中的版本生效；删除后回退到全局安装版本；再删除后回退到内置版本

#### Scenario: 缺少 frontmatter 的非法文件
- **WHEN** 技能目录中存在不含 `name` 字段的 Markdown 文件
- **THEN** 该文件被忽略，不进入技能目录，也不影响其他技能

### Requirement: 技能目录注入

系统 SHALL 在 Agent 的系统提示词中注入一份技能目录：每个可用技能占一行，包含其 `name` 与 `description`，用于告知模型当前可用的技能及何时应加载。

技能目录 SHALL 在每次 Agent 推理时反映最新技能集合（用户新建的技能立即生效，无需重启）。

#### Scenario: 新创建技能立即出现在目录中
- **WHEN** 用户通过对话创建技能 `meeting-minutes` 后，Agent 开始新一轮推理
- **THEN** 技能目录中包含 `meeting-minutes` 及其描述

#### Scenario: 技能目录随系统提示词提供
- **WHEN** Agent 调用模型进行推理
- **THEN** 系统提示词中包含全部可用技能的 `name` 与 `description` 列表

### Requirement: 按需加载技能正文

系统 SHALL 提供 `loadSkill` 工具：Agent 传入技能 `name`，工具返回该技能正文（不含 frontmatter）；未知 `name` 时 SHALL 返回明确错误信息。

技能正文 SHALL 作为工具结果进入对话上下文，而非常驻系统提示词（渐进式暴露）。

#### Scenario: 加载已存在技能
- **WHEN** Agent 调用 `loadSkill`，传入 `name=weekly-report`
- **THEN** 返回 `weekly-report` 的技能正文（不含 frontmatter 字段）

#### Scenario: 加载不存在的技能
- **WHEN** Agent 调用 `loadSkill`，传入未注册的 `name`
- **THEN** 返回包含"技能不存在"含义的错误信息，不产生其他副作用

### Requirement: 通过对话创建技能

系统 SHALL 预置名为 `skill-creator` 的内置技能，其正文指导 Agent 如何为用户创建新技能：从用户描述中提取 `name` 与 `description`、组织技能正文，并通过既有文件工具在 `user.dir/skills/` 下按统一格式写入 `<name>/SKILL.md` 文件。

创建技能的文件写入 SHALL 受既有沙盒路径校验约束（仅允许 `user.dir` 范围内路径，禁止 `~`、绝对路径等越界形式）；同一 `name` 已存在时 SHALL 直接覆盖，不做冲突提示（第一版范围）。

#### Scenario: 用户请求创建新技能
- **WHEN** 用户描述"帮我创建一个生成周报的技能"，Agent 加载 `skill-creator` 并按其指引执行
- **THEN** `user.dir/skills/weekly-report/SKILL.md` 被创建，含有效 frontmatter 与正文，且该技能随即出现在技能目录中

#### Scenario: 覆盖同名技能
- **WHEN** 用户再次创建同名技能 `weekly-report` 且内容不同
- **THEN** 新内容覆盖旧文件，技能目录仍只显示一个 `weekly-report`

#### Scenario: 创建路径越界被拒绝
- **WHEN** 创建过程中尝试将技能文件写入 `user.dir` 之外
- **THEN** 既有沙盒校验拒绝该操作，返回"操作被拒绝：路径越界"，文件不落盘

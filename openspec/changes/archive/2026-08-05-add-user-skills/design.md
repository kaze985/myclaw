## Context

MyClaw 当前所有工具均为编译期静态注册：`ToolRegistration.allTools()` 通过构造函数注入 11 个 `@Tool` 组件并统一注册（见 proposal.md - Impact 与现有 `ToolRegistration`）。关键现状约束：

- `ToolCallAgent.think()` 每次推理都调用 `chatClient.prompt(prompt).system(getSystemPrompt()).toolCallbacks(availableTools)`——system prompt 每次动态传入，天然支持"目录随推理刷新"。
- `SandboxedFileOpsTool` 已提供 `readFile` / `writeFile` / `listDirectory` / `createDirectory` 四个沙盒内工具（`isPathSafe` 校验 `user.dir` 前缀），可完整承载"创建技能文件"的写入需求，无需新增写权限。
- 项目基于 Spring Boot 3.5，自带 SnakeYAML（spring-boot-starter 传递依赖），解析 frontmatter 无需新增依赖。

## Goals / Non-Goals

**Goals:**
- 双来源（classpath 内置 + `user.dir/skills/` 用户目录）的技能存储与加载。
- 轻量目录注入 system prompt + `loadSkill` 按需加载，上下文不被技能正文污染。
- `skill-creator` 走与普通技能完全相同的机制（无特权入口），创建动作复用既有沙盒文件工具。

**Non-Goals:**
- 重名检测、编辑、删除、禁用、数量上限、正文长度上限（第一版明确砍掉，同名直接覆盖）。
- 语义检索 / embedding 索引（目录 + 按需加载已满足渐进式暴露）。
- skill 内容的静态校验（如"禁止提及工具调用"）——完全信任用户。

## Decisions

### D1: 三来源技能仓库 `SkillRepository`（classpath 内置 + 全局安装 + 项目用户，嵌套格式）

统一封装三类来源的查找：`listCatalog()` 返回全部技能元信息（`name` + `description`），`load(name)` 返回正文。classpath 通过 `classpath*:skills/**/*.md` 扫描，文件系统来源（`user.dir/skills` 与 `user.home/.agents/skills`）通过递归 `File` 遍历。目录格式统一为 `<根>/<name>/SKILL.md`，读侧兼容旧扁平 `<name>.md`。同 name 冲突时优先级：项目用户目录 > 全局安装目录 > 内置。

- 备选：仅用户目录（内置 skill-creator 则需硬编码进 system prompt）——否决，违背"skill-creator 也是普通技能"的决策，且失去内置技能可扩展性。
- 备选：不支持 `user.home/.agents/skills`——否决，用户需要通过 `npx skills add` 安装现成技能（如 grill-me、find-skills），这是技能生态的主要来源。

### D2: frontmatter 用 Spring Boot 自带 SnakeYAML 解析

以 `---` 定界提取 frontmatter 块，`org.yaml.snakeyaml.Yaml` 反序列化为 `Map`，缺失 `name` 的文件跳过并记日志。正文为定界符之后的内容。

- 备选：手写正则解析——脆，无法处理引号/多行值。
- 备选：新增独立 YAML 依赖——无必要，Boot 已带。

### D3: `loadSkill` 独立工具（`LoadSkillTool`），不直接复用 `readFile`

复用 `readFile` 的路径泄露了存储细节（模型要自己拼 `skills/<name>.md` 路径，且拿不到 classpath 资源）。独立 `@Tool(description="...")` 方法 `loadSkill(String name)` 内部走 `SkillRepository.load(name)`，返回正文或"技能不存在：<name>"。注册进 `ToolRegistration.allTools()`，与既有 11 个工具并列。

### D4: 技能目录在 system prompt 中动态拼接

`SkillCatalogGenerator` 生成目录文本（每行 `- name: description`，附一句"如需使用某技能请调用 loadSkill 加载其指令"），`MyClaw` 构造时将目录拼入 SYSTEM_PROMPT 尾部。由于 `think()` 每次重新传入 system prompt，用户新创建的技能在下一轮推理自动生效（无缓存失效问题；文件量级小，每轮扫描可接受，后续可加缓存）。

- 备选：启动时一次性生成并缓存——技能目录会滞后于运行时创建，违背 spec"每次推理反映最新集合"。
- 边界：目录行数无上限（第一版砍掉数量上限），文件极多时目录膨胀——已接受，记入风险。

### D5: `skill-creator` 为预置普通技能，内容指向复用沙盒工具

新增 `src/main/resources/skills/skill-creator/SKILL.md`：frontmatter `name: skill-creator`、`description` 含"创建/定义/新增技能"等触发词；正文写清创建流程（提取 name/description → 组织 Markdown 正文 → 用 `createDirectory` 创建 `skills/<name>` 目录 → 用 `writeFile` 写入 `skills/<name>/SKILL.md` → 说明目录命名规则）。它通过目录被发现、通过 `loadSkill` 加载，与用户技能完全同构，无特权路径。

- 备选：专用 `createSkill` 工具（字段校验、重名检查内置）——更可控但引入特权路径与重复代码，第一版按用户决策（复用现有工具）否决。

### D6: 目录名 = `<name>`，定义文件固定为 `SKILL.md`

技能目录命名规范由 `skill-creator` 正文与创建流程共同保证：`user.dir/skills/<name>/SKILL.md`。`SkillRepository` 扫描时以 frontmatter 的 `name` 为准（文件名/目录名仅作载体，不参与身份判定）。

## Risks / Trade-offs

- **提示注入面扩大**（用户 A 创建的 skill 含恶意指令，注入全体对话）→ 第一版按设计接受（全局共享 + 完全信任）；兜底为既有终端白名单与沙盒路径校验；第二版补：数量/长度上限、禁工具调用类指令、按创建者隔离。
- **劣质/冲突技能永久驻留**（同名直接覆盖、无删除入口）→ 接受；skill 文件即用户可手动管理的普通文件，可在服务器上直接编辑删除。
- **目录膨胀**（技能无上限，目录随数量线性增长稀释注意力）→ 接受（第一版砍掉上限）；第二版可加目录截断或检索。
- **frontmatter 解析失败静默跳过** → 日志记录 + 目录不展示该技能，行为可观测、可恢复（用户重写文件）。

## Migration Plan

1. 新增代码与资源均为增量（新包 + 新类 + 新资源文件 + `ToolRegistration` 增一行注册 + `MyClaw` 拼接目录），无既有行为变更，无 schema/配置迁移。
2. 首次启动自动创建 `user.dir/skills/`（由 `skill-creator` 创建流程或 `SkillRepository` 初始化兜底）。
3. 回滚：移除 `loadSkill` 注册与 system prompt 目录拼接即可，用户创建的 `user.dir/skills/` 文件不影响既有功能。

## Open Questions

无——所有影响规格/方案的未知点已在需求确认阶段钉死（渐进式暴露机制、skill-creator 定位、复用现有工具、第一版范围）。

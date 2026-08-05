## 1. Skill 存储与解析层

- [x] 1.1 新增 `tool/skill/SkillRepository`：扫描 classpath `skills/*.md` 与 `user.dir/skills/*.md`，同 name 用户目录优先，提供 `listCatalog()`（name + description）与 `load(name)`（正文，不含 frontmatter）
- [x] 1.2 实现 frontmatter 解析：`---` 定界提取，用 SnakeYAML 解析 `name` / `description`，缺失 `name` 的文件跳过并记日志
- [x] 1.3 初始化时确保 `user.dir/skills/` 目录存在（不存在则创建）

## 2. loadSkill 工具与注册

- [x] 2.1 新增 `tool/skill/LoadSkillTool`：`@Tool` 方法 `loadSkill(String name)`，走 `SkillRepository.load(name)`，未知 name 返回"技能不存在：<name>"
- [x] 2.2 在 `ToolRegistration` 中注入 `LoadSkillTool` 并加入 `allTools()` 注册

## 3. skill-creator 与目录注入

- [x] 3.1 新增资源 `src/main/resources/skills/skill-creator.md`：frontmatter 含触发词描述，正文写明创建流程（提取 name/description → 组织正文 → `createDirectory` 确保 `skills` 目录 → `writeFile` 写入 `skills/<name>.md`，命名规则 `<name>.md`）
- [x] 3.2 新增 `SkillCatalogGenerator`：生成目录文本（每行 `- name: description` + 使用指引），`MyClaw` 将其拼入 SYSTEM_PROMPT 尾部

## 4. 验证

- [x] 4.1 单元测试：frontmatter 解析（正常/缺 name/空文件）、同 name 用户目录优先、未知 name 返回错误
- [x] 4.2 集成验证：启动应用，确认 `loadSkill` 在工具列表、`skill-creator` 出现在目录中；通过对话创建技能后新技能下一轮即出现在目录并可加载

## 5. 全局来源与统一目录格式

- [x] 5.1 `SkillRepository` 扩展为三来源：classpath 内置、`user.home/.agents/skills`（全局安装）、`user.dir/skills`（项目用户），优先级：项目用户 > 全局 > 内置
- [x] 5.2 统一目录格式为 `<根>/<name>/SKILL.md`（递归扫描 + 嵌套优先加载），读侧兼容旧扁平 `<name>.md`
- [x] 5.3 内置 `skill-creator` 迁移至 `skills/skill-creator/SKILL.md`，正文改为指引创建嵌套格式并禁止 `~`/绝对路径
- [x] 5.4 测试：嵌套格式扫描/加载、全局来源识别、三来源优先级、既有用例回归

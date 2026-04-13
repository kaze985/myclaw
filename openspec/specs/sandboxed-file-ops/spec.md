## ADDED Requirements

### Requirement: Agent 只能在 user.dir 沙盒内进行文件操作
系统 SHALL 提供一组 `@Tool` 标注的文件操作工具方法，包括：读取文件内容、写入文件内容、列出目录文件、创建目录。

所有操作 SHALL 在执行前对目标路径进行规范化（`Path.toAbsolutePath().normalize()`），并验证规范化后的路径以 `System.getProperty("user.dir")` 开头；若路径越界，SHALL 拒绝操作并返回错误信息 "操作被拒绝：路径越界"。

系统 SHALL 不提供任何删除文件或目录的工具方法。

#### Scenario: 在 user.dir 内读取文件
- **WHEN** Agent 调用读取工具，传入 `user.dir` 范围内的有效文件路径
- **THEN** 系统返回该文件的文本内容字符串

#### Scenario: 在 user.dir 内写入文件
- **WHEN** Agent 调用写入工具，传入合法路径和内容字符串
- **THEN** 系统将内容写入指定文件，返回 "写入成功" 确认字符串

#### Scenario: 尝试访问 user.dir 外的路径被拒绝
- **WHEN** Agent 传入路径 `../../etc/passwd` 或任何规范化后超出 `user.dir` 的路径
- **THEN** 工具返回 "操作被拒绝：路径越界"，不执行任何文件操作

#### Scenario: 列出目录内容
- **WHEN** Agent 调用列目录工具，传入 `user.dir` 范围内的目录路径
- **THEN** 系统返回该目录下文件和子目录名称列表

#### Scenario: 删除操作不可用
- **WHEN** Agent 尝试调用不存在的删除工具
- **THEN** Agent Framework 无法发现删除工具，操作无法执行

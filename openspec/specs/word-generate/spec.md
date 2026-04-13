## ADDED Requirements

### Requirement: Agent 可根据内容生成 Word 文档
系统 SHALL 提供一个 `@Tool` 标注的工具方法，接受文件名（不含扩展名）和结构化内容（标题列表 + 段落文本），使用 Apache POI 生成 `.docx` 格式 Word 文档，保存到 `{user.dir}/tmp/file/{filename}.docx`，并返回该文件的绝对路径。

工具 SHALL 支持传入段落列表，每个段落可指定标题级别（1-3 级）或正文。

#### Scenario: 成功生成 Word 文档
- **WHEN** Agent 调用 Word 生成工具，传入文件名 "summary" 和多段内容（含一级标题、正文段落）
- **THEN** 系统生成 `{user.dir}/tmp/file/summary.docx`，文件大小大于 0，工具返回该文件绝对路径

#### Scenario: 输出目录不存在时自动创建
- **WHEN** `{user.dir}/tmp/file` 目录不存在
- **THEN** 系统自动创建该目录后生成文件

#### Scenario: 内容为空时生成空文档
- **WHEN** Agent 传入空内容列表
- **THEN** 系统生成一个有效但内容为空的 `.docx` 文件，不抛出异常

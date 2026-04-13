## ADDED Requirements

### Requirement: Agent 可根据内容生成 PDF 文件
系统 SHALL 提供一个 `@Tool` 标注的工具方法，接受文件名（不含扩展名）和 HTML 格式的内容字符串，使用 Flying Saucer + OpenPDF 将内容渲染为 PDF，保存到 `{user.dir}/tmp/file/{filename}.pdf`，并返回该文件的绝对路径。

生成的 PDF SHALL 支持中文字符显示（需配置中文字体）。

#### Scenario: 成功生成 PDF 文件
- **WHEN** Agent 调用 PDF 生成工具，传入文件名 "report" 和包含中文的 HTML 内容
- **THEN** 系统生成 `{user.dir}/tmp/file/report.pdf`，文件大小大于 0，工具返回该文件绝对路径

#### Scenario: 输出目录不存在时自动创建
- **WHEN** `{user.dir}/tmp/file` 目录不存在
- **THEN** 系统自动创建该目录后生成文件

#### Scenario: HTML 内容格式错误时返回错误信息
- **WHEN** Agent 传入无法解析的 HTML 内容
- **THEN** 工具返回包含错误描述的字符串，不抛出未处理异常

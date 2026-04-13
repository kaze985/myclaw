## ADDED Requirements

### Requirement: Agent 可根据内容生成 PPT 演示文稿
系统 SHALL 提供一个 `@Tool` 标注的工具方法，接受文件名（不含扩展名）和幻灯片列表（每张幻灯片含标题和要点列表），使用 Apache POI（XSLF）生成 `.pptx` 格式演示文稿，保存到 `{user.dir}/tmp/file/{filename}.pptx`，并返回该文件的绝对路径。

每张幻灯片 SHALL 包含标题文本框和内容文本框（要点以换行符分隔）。

#### Scenario: 成功生成 PPT 文件
- **WHEN** Agent 调用 PPT 生成工具，传入文件名 "presentation" 和 3 张幻灯片的内容
- **THEN** 系统生成 `{user.dir}/tmp/file/presentation.pptx`，文件包含 3 张幻灯片，工具返回文件绝对路径

#### Scenario: 输出目录不存在时自动创建
- **WHEN** `{user.dir}/tmp/file` 目录不存在
- **THEN** 系统自动创建该目录后生成文件

#### Scenario: 幻灯片列表为空时生成空演示文稿
- **WHEN** Agent 传入空幻灯片列表
- **THEN** 系统生成一个有效但无幻灯片的 `.pptx` 文件，不抛出异常

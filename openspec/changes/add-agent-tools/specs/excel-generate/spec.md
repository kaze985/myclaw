## ADDED Requirements

### Requirement: Agent 可根据内容生成 Excel 电子表格
系统 SHALL 提供一个 `@Tool` 标注的工具方法，接受文件名（不含扩展名）、表头列表和数据行列表（每行为字符串列表），使用 Apache POI 生成 `.xlsx` 格式电子表格，保存到 `{user.dir}/tmp/file/{filename}.xlsx`，并返回该文件的绝对路径。

工具 SHALL 将表头渲染为加粗样式的首行，数据行按传入顺序写入。

#### Scenario: 成功生成 Excel 文件
- **WHEN** Agent 调用 Excel 生成工具，传入文件名 "data"、表头 ["姓名","年龄"] 和 3 行数据
- **THEN** 系统生成 `{user.dir}/tmp/file/data.xlsx`，文件含表头行和 3 条数据行，工具返回文件绝对路径

#### Scenario: 输出目录不存在时自动创建
- **WHEN** `{user.dir}/tmp/file` 目录不存在
- **THEN** 系统自动创建该目录后生成文件

#### Scenario: 数据行为空时只生成表头
- **WHEN** Agent 传入表头但数据行列表为空
- **THEN** 系统生成只有表头行的有效 `.xlsx` 文件，不抛出异常

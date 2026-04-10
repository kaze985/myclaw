## ADDED Requirements

### Requirement: Agent 可通过 URL 下载文件到本地
系统 SHALL 提供一个 `@Tool` 标注的工具方法，接受文件 URL 字符串，使用 Hutool `HttpUtil.downloadFile()` 将文件下载到 `{user.dir}/tmp/file` 目录，并返回保存后的绝对文件路径字符串。

目标目录 `{user.dir}/tmp/file` SHALL 在下载前自动创建（若不存在）。

文件名 SHALL 优先从 URL 最后一个路径段提取；若无法确定扩展名，SHALL 追加时间戳作为文件名。

#### Scenario: 成功下载可访问的文件
- **WHEN** Agent 调用下载工具，传入一个可访问的文件 URL（如 PDF、图片）
- **THEN** 文件被保存到 `{user.dir}/tmp/file/` 目录，工具返回该文件的绝对路径

#### Scenario: tmp/file 目录不存在时自动创建
- **WHEN** `{user.dir}/tmp/file` 目录不存在，Agent 调用下载工具
- **THEN** 系统自动创建该目录，文件正常下载

#### Scenario: URL 无效或无法访问时返回错误
- **WHEN** Agent 传入无效 URL 或 HTTP 响应为非 2xx
- **THEN** 工具返回错误描述字符串，不保存损坏文件，不抛出未处理异常

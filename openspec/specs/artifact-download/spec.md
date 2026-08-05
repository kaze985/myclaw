# artifact-download Specification

## Purpose

在沙盒约束内将工具生成的文档产物（PDF/Word/PPT/Excel）以只读方式暴露为可下载 URL，供 Web 聊天界面将产物路径渲染为可点击的下载链接。

## Requirements

### Requirement: 产物只读下载

系统 SHALL 提供 `GET /api/files/**` 端点，仅允许读取 `${user.dir}/tmp/file` 目录内的文件，并按扩展名返回对应的 Content-Type（如 PDF、Word、PPT、Excel），以附件形式提供下载。

#### Scenario: 下载存在的产物

- **WHEN** 已认证用户请求 `GET /api/files/<合法相对路径>` 且文件存在于 `tmp/file` 目录内
- **THEN** 系统 SHALL 返回文件内容，Content-Type 按扩展名推断，响应头标识为附件下载

#### Scenario: 产物不存在

- **WHEN** 已认证用户请求的路径在 `tmp/file` 内但文件不存在
- **THEN** 系统 SHALL 返回 404

#### Scenario: 未认证请求

- **WHEN** 未携带有效会话 cookie 的请求访问 `GET /api/files/**`
- **THEN** 系统 SHALL 返回 401，不 SHALL 读取任何文件

### Requirement: 路径沙盒校验

系统 SHALL 校验 `GET /api/files/**` 的所有请求路径，解析后的绝对路径 MUST 位于 `${user.dir}/tmp/file` 目录之内（拒绝目录穿越与符号链接逃逸）；越界请求 SHALL 返回 403。

#### Scenario: 目录穿越攻击

- **WHEN** 请求路径包含 `..` 或试图访问 `tmp/file` 之外的绝对路径
- **THEN** 系统 SHALL 返回 403，不 SHALL 返回任何文件内容

#### Scenario: 仅允许文件读取

- **WHEN** 请求指向目录或使用非 GET 方法
- **THEN** 系统 SHALL 拒绝访问（404/405），不 SHALL 支持写入、删除或目录列举

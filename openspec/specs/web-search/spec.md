## ADDED Requirements

### Requirement: Agent 可通过 Tavily 进行联网搜索
系统 SHALL 提供一个 `@Tool` 标注的工具方法，接受用户查询字符串，使用 Hutool `HttpRequest` 调用 Tavily Search REST API（`https://api.tavily.com/search`）并返回结构化搜索结果（标题、URL、摘要）列表。

HTTP 请求 SHALL 使用 Hutool `HttpRequest.post(url).body(jsonBody).timeout(10000).execute()` 完成，不得引入其他 HTTP 客户端库。

API Key SHALL 从 `application.yml` 中的 `tools.tavily.api-key` 配置项读取，不得硬编码。

搜索结果 SHALL 返回最多 5 条相关结果，每条包含 `title`、`url`、`content` 字段。

#### Scenario: 正常联网搜索返回结果
- **WHEN** Agent 调用搜索工具，传入查询字符串 "Spring AI 最新版本"
- **THEN** 系统调用 Tavily API 并返回包含至少 1 条结果的列表，每条结果含 title、url、content

#### Scenario: API Key 未配置时抛出明确错误
- **WHEN** `tools.tavily.api-key` 配置项为空或缺失
- **THEN** 工具方法抛出包含 "Tavily API Key not configured" 描述的异常

#### Scenario: Tavily API 请求超时
- **WHEN** Tavily API 在 10 秒内未响应
- **THEN** 工具返回错误信息字符串 "搜索超时，请稍后重试"，不抛出未处理异常

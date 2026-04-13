## ADDED Requirements

### Requirement: Agent 可抓取动态网页内容
系统 SHALL 提供一个 `@Tool` 标注的工具方法，接受目标 URL 字符串，采用两层策略返回页面的纯文本内容（去除 HTML 标签）：

1. **静态优先**：优先使用 Hutool `HttpUtil.get(url)` 发起 HTTP 请求获取 HTML，通过字符串处理提取文本内容；若返回内容有效（长度大于 0 且含可读文本），直接返回。
2. **动态回退**：当 Hutool 获取的内容为空或疑似未渲染（如仅含 `<script>` 标签）时，自动回退到 Playwright 无头浏览器，等待页面达到 `networkidle` 状态后提取内容。

所有底层 HTTP 网络请求 SHALL 使用 Hutool 完成；Playwright 仅负责 JS 执行和浏览器渲染，不替代 HTTP 层。

单次抓取超时 SHALL 设置为 30 秒。

#### Scenario: 成功抓取动态页面内容
- **WHEN** Agent 调用网页抓取工具，传入一个 JavaScript 渲染的 SPA 页面 URL
- **THEN** 系统返回该页面渲染后的纯文本内容，内容长度大于 0

#### Scenario: URL 不可访问时返回错误信息
- **WHEN** Agent 传入一个无法访问的 URL（如 `http://invalid.example.com`）
- **THEN** 工具返回错误描述字符串，不抛出未处理异常

#### Scenario: 页面加载超时
- **WHEN** 目标页面在 30 秒内未完成加载
- **THEN** 工具返回 "页面加载超时" 错误信息，并关闭浏览器资源

#### Scenario: 抓取完成后浏览器资源被释放
- **WHEN** 任意一次抓取完成（无论成功或失败）
- **THEN** Playwright Browser/Context/Page 实例均被正确关闭，不发生资源泄漏

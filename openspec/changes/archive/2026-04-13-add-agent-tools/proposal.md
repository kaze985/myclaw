## Why

当前系统的 AI Agent 缺乏与外部世界交互的能力，无法进行联网搜索、网页抓取、文件操作和文档生成等基础工具操作，严重制约了 Agent 的实用性。通过引入一套完整的工具集，使 Agent 具备执行真实任务的能力。

## What Changes

- **新增** Tavily 联网搜索工具，支持实时网络检索
- **新增** 动态网页抓取工具，支持 JavaScript 渲染页面内容提取
- **新增** 资源文件下载工具，基于 Hutool 实现，文件存储到 `tmp/file` 目录
- **新增** 终端命令执行工具，兼容 Windows 和 Linux 双平台
- **新增** 沙盒文件操作工具，限制 Agent 只能访问 `user.dir` 下的文件，禁止删除操作
- **新增** PDF 文档生成工具
- **新增** Word 文档生成工具
- **新增** PPT 演示文稿生成工具
- **新增** Excel 电子表格生成工具

## Capabilities

### New Capabilities

- `web-search`: 使用 Tavily API 进行联网搜索，返回相关网页摘要和链接
- `web-scrape`: 抓取动态网页内容，支持 JavaScript 渲染页面
- `resource-download`: 通过 URL 下载文件资源，存储至 `tmp/file` 目录
- `terminal-execute`: 执行终端命令，自动适配 Windows（PowerShell/CMD）和 Linux（bash）
- `sandboxed-file-ops`: 隔离的文件系统操作，限制在 `user.dir` 范围内，禁止删除
- `pdf-generate`: 根据内容生成 PDF 文档
- `word-generate`: 根据内容生成 Word（.docx）文档
- `ppt-generate`: 根据内容生成 PPT（.pptx）演示文稿
- `excel-generate`: 根据内容生成 Excel（.xlsx）电子表格

### Modified Capabilities

（无现有能力需要修改）

## Impact

- **依赖新增**：Playwright（动态网页 JS 渲染）、Apache PDFBox 或 iText（PDF）、Apache POI（Word/Excel/PPT）；所有 HTTP 网络请求（Tavily 搜索、静态网页抓取、文件下载）均复用已有 Hutool `HttpUtil`/`HttpRequest`，无需引入 OkHttp 或其他 HTTP 客户端
- **配置新增**：`application.yml` 中需新增 Tavily API Key 配置项（`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`）、`tmp/file` 目录路径配置
- **安全边界**：文件操作工具需实现路径校验，确保所有操作被限制在 `user.dir` 内；禁用删除相关 API
- **平台兼容**：终端工具需在运行时检测操作系统并切换命令执行器

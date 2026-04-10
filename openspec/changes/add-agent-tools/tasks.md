## 1. 项目基础配置

- [ ] 1.1 在 `pom.xml` 中新增 Playwright 依赖（`com.microsoft.playwright:playwright`）；确认 HTTP 网络请求均由 Hutool 承担，无需新增其他 HTTP 客户端
- [ ] 1.2 在 `pom.xml` 中新增 Apache POI 依赖（`org.apache.poi:poi-ooxml`）
- [ ] 1.3 在 `pom.xml` 中新增 Flying Saucer + OpenPDF 依赖（`org.xhtmlrenderer:flying-saucer-pdf-openpdf`）
- [ ] 1.4 在 `application.yml` 中新增 `tools.tavily.api-key` 配置项
- [ ] 1.5 在 `application.yml` 中新增 `tools.file.download-dir` 配置项（默认值：`${user.dir}/tmp/file`）
- [ ] 1.6 在 `application.yml` 中新增 `tools.terminal.allowed-commands` 配置项（初始默认白名单：`echo,ls,dir,pwd,cat,grep,curl,java,mvn`）
- [ ] 1.7 在 `com.lppnb.ai.myclaw.agent.tool` 下创建子包：`search`、`scrape`、`download`、`terminal`、`file`、`pdf`、`word`、`ppt`、`excel`
- [ ] 1.8 在 CI/CD 流水线部署步骤中加入 Playwright Chromium 预安装脚本（`mvn exec:java -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"`）

## 2. 联网搜索工具（web-search）

- [ ] 2.1 创建 `WebSearchTool` 类，注入 Tavily API Key 配置
- [ ] 2.2 实现 `@Tool` 方法 `webSearch(String query)`，通过 Hutool `HttpRequest.post()` 调用 Tavily REST API
- [ ] 2.3 封装返回结果为结构化字符串（title + url + content，最多 5 条）
- [ ] 2.4 实现 10 秒超时处理，超时返回友好错误信息
- [ ] 2.5 实现 API Key 未配置时的前置校验和异常抛出

## 3. 动态网页抓取工具（web-scrape）

- [ ] 3.1 创建 `WebScrapeTool` 类
- [ ] 3.2 实现 `@Tool` 方法 `webScrape(String url)`，优先使用 Hutool `HttpUtil.get(url)` 获取 HTML 并提取文本
- [ ] 3.3 实现静态内容有效性判断逻辑（内容为空或疑似未渲染时触发动态回退）
- [ ] 3.4 实现动态回退：使用 Playwright 启动 Chromium 无头浏览器，等待 `networkidle` 后提取 `innerText`
- [ ] 3.5 实现 30 秒超时处理和 Playwright 资源关闭（try-with-resources 确保 Browser/Context/Page 释放）
- [ ] 3.6 捕获异常并返回错误描述字符串

## 4. 资源下载工具（resource-download）

- [ ] 4.1 创建 `ResourceDownloadTool` 类，注入下载目录配置
- [ ] 4.2 实现 `@Tool` 方法 `downloadResource(String url)`，应用启动时确保下载目录存在
- [ ] 4.3 使用 Hutool `HttpUtil.downloadFile()` 执行文件下载
- [ ] 4.4 从 URL 末段推断文件名，无法确定时使用时间戳命名
- [ ] 4.5 返回下载文件的绝对路径字符串，异常时返回错误描述

## 5. 终端命令执行工具（terminal-execute）

- [ ] 5.1 创建 `TerminalExecuteTool` 类
- [ ] 5.2 实现 `@Tool` 方法 `executeCommand(String command)`
- [ ] 5.3 实现命令白名单校验：从配置读取 `tools.terminal.allowed-commands`，提取命令首词与白名单比对，不匹配则返回拒绝信息
- [ ] 5.4 通过 `System.getProperty("os.name")` 判断 OS，构建相应命令数组（Windows/Linux）
- [ ] 5.5 使用 `ProcessBuilder` 执行命令，合并 stdout 和 stderr
- [ ] 5.6 实现 30 秒超时（`process.waitFor(30, TimeUnit.SECONDS)`），超时强制销毁进程
- [ ] 5.7 返回格式化字符串 `[exitCode=N]\n{output}`

## 6. 沙盒文件操作工具（sandboxed-file-ops）

- [ ] 6.1 创建 `SandboxedFileOpsTool` 类，实现路径安全校验私有方法（规范化 + 前缀验证）
- [ ] 6.2 实现 `@Tool` 方法 `readFile(String path)`，读取文件内容并返回字符串
- [ ] 6.3 实现 `@Tool` 方法 `writeFile(String path, String content)`，写入文件内容
- [ ] 6.4 实现 `@Tool` 方法 `listDirectory(String path)`，列出目录内容
- [ ] 6.5 实现 `@Tool` 方法 `createDirectory(String path)`，创建目录
- [ ] 6.6 所有方法均在操作前调用安全校验，越界时返回 "操作被拒绝：路径越界"
- [ ] 6.7 确认未暴露任何删除相关方法

## 7. PDF 生成工具（pdf-generate）

- [ ] 7.1 创建 `PdfGenerateTool` 类，注入下载目录配置
- [ ] 7.2 实现 `@Tool` 方法 `generatePdf(String filename, String htmlContent)`
- [ ] 7.3 使用 Flying Saucer `ITextRenderer` 将 HTML 渲染为 PDF
- [ ] 7.4 配置中文字体（classpath 中放置字体文件并在渲染器中注册）
- [ ] 7.5 输出文件到 `{user.dir}/tmp/file/{filename}.pdf`，返回绝对路径
- [ ] 7.6 捕获渲染异常并返回错误描述

## 8. Word 文档生成工具（word-generate）

- [ ] 8.1 创建 `WordGenerateTool` 类，注入下载目录配置
- [ ] 8.2 定义内部数据模型 `Paragraph`（含 level 字段：0=正文，1-3=标题级别）
- [ ] 8.3 实现 `@Tool` 方法 `generateWord(String filename, List<Paragraph> paragraphs)`
- [ ] 8.4 使用 Apache POI `XWPFDocument` 按段落列表生成 `.docx` 文件
- [ ] 8.5 输出文件到 `{user.dir}/tmp/file/{filename}.docx`，返回绝对路径

## 9. PPT 演示文稿生成工具（ppt-generate）

- [ ] 9.1 创建 `PptGenerateTool` 类，注入下载目录配置
- [ ] 9.2 定义内部数据模型 `Slide`（含 title 和 bullets 列表）
- [ ] 9.3 实现 `@Tool` 方法 `generatePpt(String filename, List<Slide> slides)`
- [ ] 9.4 使用 Apache POI `XMLSlideShow` 创建 `.pptx` 文件，每张 Slide 含标题和内容文本框
- [ ] 9.5 输出文件到 `{user.dir}/tmp/file/{filename}.pptx`，返回绝对路径

## 10. Excel 电子表格生成工具（excel-generate）

- [ ] 10.1 创建 `ExcelGenerateTool` 类，注入下载目录配置
- [ ] 10.2 实现 `@Tool` 方法 `generateExcel(String filename, List<String> headers, List<List<String>> rows)`
- [ ] 10.3 使用 Apache POI `XSSFWorkbook` 创建 `.xlsx` 文件，表头行设置加粗样式
- [ ] 10.4 按传入顺序写入数据行
- [ ] 10.5 输出文件到 `{user.dir}/tmp/file/{filename}.xlsx`，返回绝对路径

## 11. 工具注册与集成验证

- [ ] 11.1 在 `ToolRegistration.allTools()` 中追加全部 9 个新工具实例，与已有 `TerminateTool` 一起通过 `ToolCallbacks.from(...)` 统一注册
- [ ] 11.2 确认应用启动后 `allTools` Bean 包含所有新工具，无 Bean 冲突或初始化异常
- [ ] 11.3 编写集成测试：验证 Agent 可正确调用 webSearch 工具并返回结果
- [ ] 11.4 验证沙盒文件工具的路径越界防护在测试中通过
- [ ] 11.5 验证文档生成工具输出文件格式正确（可用对应 SDK 重新读取验证）

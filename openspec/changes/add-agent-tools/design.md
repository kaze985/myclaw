## Context

项目基于 Spring Boot 3.5.11 + Spring AI Alibaba 1.1.2.2（DashScope + Agent Framework）构建，已集成 Hutool 5.8.44、Knife4j 和 Lombok。需要在 Agent Framework 的工具（Tool）机制下新增 9 个实用工具能力，供 AI Agent 调用。

当前约束：
- Java 21，Maven 构建
- Spring AI Alibaba Agent Framework 是工具注册和调用的核心机制
- Hutool 已存在，优先复用
- 需同时支持 Windows 和 Linux 运行环境

## Goals / Non-Goals

**Goals:**
- 在 Spring AI Alibaba Tool 机制下实现 9 个工具，每个工具以 `@Tool` 注解方法形式暴露
- 工具均可被 AI Agent 自动发现和调用（通过 `@Component` + `ToolCallbackProvider`）
- 文件操作严格隔离在 `user.dir` 目录下，禁止删除
- 终端工具自动适配操作系统
- 所有文档生成工具输出到 `tmp/file` 目录

**Non-Goals:**
- 不实现工具的权限认证体系（由 Agent Framework 层处理）
- 不实现文档预览/渲染 UI
- 不支持浏览器自动化测试（Web 抓取仅用于数据提取）

## Decisions

### 1. 工具注册机制：`@Tool` 注解 + `ToolRegistration.allTools()` 统一注册
**选择**：每个工具类放置于 `com.lppnb.ai.myclaw.agent.tool.<子包>` 下，工具方法标注 `@Tool(description = "...")`。所有新工具实例统一在已有的 `ToolRegistration.allTools()` Bean 方法中通过 `ToolCallbacks.from(...)` 注册，与现有 `TerminateTool` 保持一致的注册模式。

包结构约定：
```
agent/tool/
├── ToolRegistration.java       # 统一注册入口（已有）
├── special/TerminateTool.java          # 已有
├── search/WebSearchTool.java
├── scrape/WebScrapeTool.java
├── download/ResourceDownloadTool.java
├── terminal/TerminalExecuteTool.java
├── file/SandboxedFileOpsTool.java
├── pdf/PdfGenerateTool.java
├── word/WordGenerateTool.java
├── ppt/PptGenerateTool.java
└── excel/ExcelGenerateTool.java
```

**理由**：复用现有 `ToolRegistration` 注册模式，统一管理所有工具，无需引入自动扫描或额外配置；按功能子包分组，结构清晰，便于维护。

**备选**：`@Component` 自动扫描注入 → 工具注册分散，不便于统一管控。

### 2. 联网搜索：Tavily REST API（Hutool `HttpRequest` 调用）
**选择**：通过 Hutool `HttpRequest.post("https://api.tavily.com/search")` 调用 Tavily Search REST API，请求体为 JSON，无需引入独立 SDK 或额外 HTTP 客户端。

**理由**：Hutool 已作为项目依赖存在，`HttpRequest` 支持 POST JSON、设置超时、读取响应体，完全满足调用 REST API 的需求，零额外引入。API Key 通过 `application.yml` 的 `tools.tavily.api-key` 配置注入。

**备选**：OkHttp / RestTemplate → 需额外引入或配置 Bean，违背复用 Hutool 的约束；Tavily Java 社区 SDK → 版本不稳定，维护风险高。

### 3. 动态网页抓取：Hutool（静态）+ Playwright（动态 JS 渲染）
**选择**：网页抓取采用两层策略：
- **静态页面**：优先使用 Hutool `HttpUtil.get(url)` 直接获取 HTML，再通过 Hutool `ReUtil` / 字符串处理提取文本内容，零额外依赖。
- **动态页面（JS 渲染）**：当静态抓取内容为空或 Agent 明确指定需要 JS 渲染时，回退到 Playwright 驱动 Chromium 无头浏览器等待 `networkidle` 后提取内容。

**理由**：Hutool HTTP 层负责所有基础网络请求，Playwright 仅承担浏览器自动化（JS 执行/渲染），职责分离清晰。大多数页面可由 Hutool 直接处理，仅必要时才启动 Playwright，降低资源消耗。

**备选**：Selenium → 配置繁琐，需外部 WebDriver；纯 Jsoup → 仅支持静态 HTML，无法处理 JS 渲染内容。

### 4. 文件下载：Hutool `HttpUtil.downloadFile()`
**选择**：复用已有 Hutool，使用 `HttpUtil.downloadFile(url, destFile)` 方法。下载目录固定为 `{user.dir}/tmp/file`，文件名由 URL 末段推断或使用时间戳。

**理由**：Hutool 已作为依赖存在，零额外引入，与联网搜索、网页抓取保持统一的网络层。

### 5. 终端命令执行：ProcessBuilder + OS 检测 + 命令白名单
**选择**：通过 `System.getProperty("os.name")` 判断操作系统，Windows 使用 `["cmd.exe", "/c", command]`，Linux/Mac 使用 `["/bin/sh", "-c", command]`。超时 30 秒，捕获 stdout 和 stderr。

执行前 SHALL 对命令进行白名单校验：提取命令首个词（程序名），在允许列表中查找；不在白名单的命令直接拒绝并返回错误信息，不进入 ProcessBuilder 执行。白名单通过 `application.yml` 的 `tools.terminal.allowed-commands` 配置，支持运维动态调整。

**理由**：标准 Java API，无额外依赖；白名单限制防止 Agent 执行危险命令（如 `rm -rf`、`format`），在安全性与灵活性之间取得平衡。

**备选**：Apache Commons Exec → 功能过剩，增加依赖；完全禁用终端 → 丧失工具实用性。

### 6. 文件操作沙盒：路径规范化 + 白名单校验
**选择**：所有文件路径操作前，先调用 `Path.toRealPath()` / `Path.normalize()` 解析绝对路径，再验证是否以 `System.getProperty("user.dir")` 开头。禁止暴露 `delete`、`deleteOnExit` 等破坏性 API。

**理由**：防止路径穿越攻击（如 `../../etc/passwd`），安全边界清晰。

### 7. PDF 生成：Flying Saucer + iText 5 / OpenPDF（仅程序化 API）
**选择**：使用 `org.xhtmlrenderer:flying-saucer-pdf-openpdf` 将 HTML/CSS 转换为 PDF，底层 PDF 引擎为 OpenPDF（iText 5 LGPL 分支）。HTML 内容由调用方（Agent）以字符串形式传入，工具内部维护固定的 HTML 骨架模板（含字符集、字体声明），不支持外部模板上传。

**理由**：支持中文和复杂排版，LGPL 开源协议无商业限制；固定模板降低实现复杂度，Agent 只需关注内容本身，不需要维护模板文件。

**备选**：Apache PDFBox → 低级 API，手动布局工作量大；iText 7 → AGPL 协议，商业使用需付费。

### 8. Word / Excel / PPT 生成：Apache POI
**选择**：统一使用 `org.apache.poi:poi-ooxml` 生成 `.docx`、`.xlsx`、`.pptx` 格式文件。

**理由**：业界标准，功能完整，Spring Boot 生态中最成熟的 Office 文档库。

## Risks / Trade-offs

- **Playwright 初始化耗时** → 首次调用触发浏览器下载（约 150MB），仅动态页面抓取时才启动，可通过 `PLAYWRIGHT_BROWSERS_PATH` 指向预装缓存规避
- **Playwright 在 Linux 无头服务器需要系统依赖** → 部署文档需说明 `playwright install-deps` 或使用官方 Docker 镜像；静态页面由 Hutool 处理，可完全绕过此问题
- **Hutool HTTP 不支持 JS 渲染** → 动态页面自动回退 Playwright，已在设计中明确分层
- **PDF 中文字体** → 需在 classpath 或服务器中提供中文字体文件（如思源黑体），否则中文显示为方块
- **Tavily API 限流** → 免费层有请求频率限制，生产环境需升级付费计划
- **终端命令白名单维护成本** → 白名单过窄会限制 Agent 能力，过宽则引入安全风险；初始默认值应保守，由运维按需扩展

## Migration Plan

1. 在 `pom.xml` 新增依赖（Playwright、OpenPDF、Flying Saucer、Apache POI）；HTTP 网络请求复用 Hutool，无需新增 HTTP 客户端依赖
2. 在 `application.yml` 新增工具相关配置（Tavily API Key、文件路径、终端命令白名单 `tools.terminal.allowed-commands` 等）
3. 在 `com.lppnb.ai.myclaw.agent.tool` 下按功能创建子包，每个工具类放入对应子包
4. 在 `ToolRegistration.allTools()` 中追加新工具实例（`ToolCallbacks.from(existingTools..., newTool1, newTool2, ...)`）
5. 确保 `tmp/file` 目录在应用启动时自动创建

## Resolved Decisions

- **Playwright 浏览器预安装**：使用 Playwright 官方提供的脚本（`mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"`）在部署时预安装 Chromium，纳入 CI/CD 流水线和部署文档。
- **PDF 模板**：不支持自定义 HTML 模板上传，仅提供程序化 API；工具内部维护固定 HTML 骨架，Agent 传入内容字符串即可。
- **终端命令白名单**：必须启用白名单限制，通过 `tools.terminal.allowed-commands` 配置项管理，默认值为保守的常用命令集（如 `echo`、`ls`、`dir`、`pwd`、`cat`、`grep`、`curl`、`java`、`mvn`）。

# proposal: add-web-chat-interface

## Why

目前 MyClaw 仅能通过飞书机器人交互，缺少浏览器直连的轻量入口。web 聊天界面可以复用 Agent 现有的 `onThought` 实时思考推送机制，将思考过程、工具调用与最终回复以流式方式直观呈现，显著降低试用门槛，也便于在没有飞书环境的场景下直接使用 Agent 能力。

## What Changes

- 新增独立前端工程 `web/`（Vue 3 + Vite + TypeScript），构建产物由 Maven 集成复制进 Spring Boot 静态目录，保持单进程启动体验
- 新增 **Web Channel**（`gateway.web` 包），复用现有 `GatewayMessage` / `AgentMessageRouter` 抽象：
  - `POST /api/chat`：SSE 流式聊天端点（`SseEmitter`），Agent 每次思考产生的文本通过 `onThought` 回调实时推送；模型流式输出逐段推送 `token` 事件（`onToken`，真·流式打字）；最终回复后推送 `done` 并关闭流
  - `POST /api/chat/new`：清空 Agent 上下文（等价于飞书 `/new` 命令）
  - 与飞书共用同一 Agent 实例与全局上下文（MVP 决策）；`AgentMessageRouter` 增加同步锁，web 与飞书消息串行处理
  - Agent 同步调用改为流式调用（`ChatClient.stream()`），`BaseAgent`/`GatewayMessage` 新增 `onToken` 回调（null 时不触发，飞书行为不变）
- 新增 **访问认证**（`web-auth`）：环境变量配置访问密码，登录成功后签发 HttpOnly cookie 会话；所有 web 端点经拦截器校验（EventSource 无法自定义 header，故不用 Bearer token）；`GET /api/auth/me` 供前端探测登录状态
- 新增 **产物下载端点**：`GET /api/files/**` 只读暴露 `${user.dir}/tmp/file` 下工具生成的文档（PDF/Word/PPT/Excel），供聊天界面渲染可下载链接；沿用沙盒路径校验
- 前端聊天界面（单页）：
  - 消息流 + 输入框 + 会话控制（新建会话 = 清空本地历史并调用 `/api/chat/new`）
  - Agent 回复按 Markdown 渲染、代码块高亮；思考/工具调用过程流式呈现（ReAct 管线）
  - **打字机效果**：最终回复随 `token` 事件逐字打出，等待期闪烁光标，点击消息跳过，尊重 `prefers-reduced-motion`，历史消息不打字
  - 聊天历史存 `localStorage`，刷新不丢
  - 亮色极简专业视觉，设计遵循 `frontend-design` / `ui-ux-pro-max` 生成的 design system
- 新增配置项：`web.enabled`、`web.access-password`（环境变量 `WEB_ACCESS_PASSWORD` 注入）
- 新增 `pom.xml` 构建集成：前端依赖与构建（node/npm 脚本 + maven-resources-plugin 复制 dist）

## Capabilities

### New Capabilities

- `web-chat-channel`: Web 通道，含 SSE 流式聊天端点、会话控制（新建/清空）、与现有 Gateway 抽象与 Agent 的对接（共享上下文、串行执行）
- `web-auth`: Web 访问认证，含密码登录、HttpOnly cookie 会话签发与校验拦截器
- `artifact-download`: 工具产物只读下载端点，在沙盒约束内暴露生成的文档文件
- `web-chat-ui`: Vue 3 前端聊天界面，含消息流、流式渲染、Markdown/代码高亮、本地历史持久化与登录门

### Modified Capabilities

（无既有 spec 的需求变更：`gateway-channel-abstraction` 仅新增一个 Channel 实现，不修改抽象要求；现有飞书通道行为不变）

## Impact

- **新增目录**：`web/`（Vue 3 + Vite + TypeScript 前端工程）
- **新增包**：`com.lppnb.ai.myclaw.gateway.web`（Web Channel、SSE 端点、认证拦截器、产物下载 Controller）
- **修改**：`AgentMessageRouter`（加锁串行）、`pom.xml`（前端构建集成）、`application.yml`（`web.*` 配置项）
- **新增配置**：`WEB_ACCESS_PASSWORD` 环境变量（访问认证，必填后启用 web 通道）
- **无 Breaking 变更**：飞书通道、Agent、工具均不受影响；`web.enabled` 默认关闭

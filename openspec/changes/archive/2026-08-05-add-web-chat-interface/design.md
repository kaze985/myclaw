# design: add-web-chat-interface

## Context

现状与约束（动机见 proposal.md）：

- 单进程 Spring Boot 应用，`server.servlet.context-path=/api`；`MyClaw`（`ToolCallAgent`）为全局单例，`contextMessages` 全局共享；`AgentMessageRouter.route()` 同步执行 Agent，飞书通道经 `CompletableFuture` 异步调用它
- `onThought` 回调仅在 `think()` 阶段触发（`ToolCallAgent.java:91`），携带模型思考文本；**工具执行（act/observe）结果不实时推送**——飞书用户只看到思考文本流 + 一次到位的最终回复
- 沙盒已存在：`${user.dir}/tmp/file` 为工具产物目录，`SandboxedFileOpsTool` 已有路径校验逻辑可复用
- 已定约束（specs）：web 与飞书共享全局上下文；Agent 串行执行；HttpOnly cookie 认证；产物只读下载；前端 localStorage 持久化

## Goals / Non-Goals

**Goals:**
- 单进程启动即可用的 web 聊天：`vite build` 产物打包进 Spring Boot，一条命令跑通
- SSE 流式：`thought`（思考/工具调用过程）实时推送 → `done`（完整回复）→ 关闭；异常推 `error`
- 认证门：密码登录签发 HttpOnly cookie，token 不暴露给 JS；未认证请求对聊天/下载端点一律 401
- 加锁串行：web 与飞书并发消息排队，不破坏现有共享上下文
- 亮色极简专业的前端视觉，具备可辨识的签名元素

**Non-Goals:**
- 多会话隔离、Agent 并发执行（后续迭代）
- WebSocket 双向通道、服务端主动推送（SSE 单向足够）
- 多用户体系与权限细分；多语言界面
- 前端自动化测试（MVP 手测 + 后端单测覆盖端点）

## Decisions

### D1. SSE 传输：POST + fetch ReadableStream 解析（不用 EventSource）

`EventSource` API 仅支持 GET，无法携带消息 body；消息文本放 query 参数会泄露进 URL/访问日志且受长度限制。采用 `fetch(url, { method: 'POST', body })` + `response.body.getReader()` 自行解析 `text/event-stream` 帧。同源请求自动携带 HttpOnly cookie，无 CORS 问题。

- 备选：GET + query 参数（弃：日志泄露、长度限制）；WebSocket（弃：引入连接管理复杂度，SSE 单向已满足）

### D2. 认证：内存 session registry + HttpOnly cookie（不引入 Spring Security）

- `POST /api/auth/login`：校验 `WEB_ACCESS_PASSWORD`，通过则生成随机 session id，存入内存 `ConcurrentHashMap<sessionId, expiresAt>`，响应 `Set-Cookie: myclaw_session=<id>; HttpOnly; SameSite=Lax; Path=/api; Max-Age=7d`
- `WebAuthInterceptor` 拦截 `/api/chat/**` 与 `/api/files/**`（放行 `/api/auth/login`），cookie 有效且未过期则放行，否则 401
- 备选：Spring Security（弃：为单密码门引入整框架过重）；Bearer token 存 localStorage（弃：spec 已定 HttpOnly cookie，且 token 在 JS 侧可被 XSS 窃取）

### D3. 串行执行：`AgentMessageRouter.route()` 加 `synchronized`

锁覆盖 `prepareAgent → agent.run → finally setOnThought(null)` 全流程（`AgentMessageRouter.java:23-40`）。飞书与 web 共用同一 router 实例，天然串行。现有飞书多消息并发互相踩状态的问题一并修复（与 spec「共享上下文与串行执行」一致）。

### D4. SSE 挂接：复用 `AgentMessageRouter.route()`，`onThought` 走 `SseEmitter`

`ChatController` 流程：

1. 建 `SseEmitter`（超时 10 分钟），注册 `onTimeout/onError/onCompletion` 兜底
2. 组装 `GatewayMessage`（`platform="web"`、`sessionId/senderId=当前会话`、`content=用户消息`、`onThought = text -> emitter.send(thought 事件)`）
3. 调 `router.route()`（内部持锁）；成功推 `done`（携带最终回复），异常推 `error`
4. `finally` 中 `emitter.complete()`

**thought 事件负载**：仅携带文本（`{ text }`）。工具名识别为前端渐进增强（见 D6），不依赖后端结构化事件——避免改动 `BaseAgent.onThought` 签名影响飞书通道行为（spec 承诺飞书行为不变）。

### D5. 产物下载：`GET /api/files/**` + 沙盒路径校验

- `ArtifactDownloadController`：取 `**` 捕获的相对路径，拼接 `${user.dir}/tmp/file` 后 `Path.normalize().startsWith(tmp/file 根)` 校验，拒绝目录穿越；仅 GET；文件不存在 404；Content-Type 按扩展名推断（MIME 映射），响应 `Content-Disposition: attachment`
- 前端将回复中 `tmp/file/xxx.ext` 样式的路径识别为下载链接（URL 编码中文文件名）

### D6. 前端工程：Vue 3 + Vite + TypeScript（Composition API + `<script setup>`）

- 无 UI 框架、无 vue-router（单页两视图切换）、无状态库——自建 design token + composables（`useAuth`、`useChat`、`useLocalHistory`）
- 依赖：`marked`（Markdown 渲染，sanitize 策略）、`highlight.js`（代码高亮）、`vite` + `@vitejs/plugin-vue`
- **签名元素「ReAct 管线」**：`thought` 事件按序渲染为横向步骤管线（每轮 think 一个节点）；对已知工具名（`webSearch`、`webScrape`、`pdfGenerate` 等 11 个，内置列表）做轻量文本匹配，命中则在节点上渲染工具徽标；当前步骤短暂脉冲动画；执行完成后管线折叠为「思考过程」区，最终回复为主内容——管线真实编码 ReAct 循环顺序（结构即信息），是界面唯一动效编排点
- 组件树：

```
App.vue
├── LoginView.vue            # 密码门
└── ChatView.vue
    ├── HeaderBar.vue        # 品牌 + 新建会话
    ├── MessageList.vue
    │   ├── UserMessage.vue
    │   └── AssistantMessage.vue
    │       ├── ReActPipeline.vue   # 签名元素
    │       └── MarkdownBody.vue    # marked + highlight.js
    └── Composer.vue         # 输入 + 发送 + 进行状态
```

### D7. 设计系统（frontend-design + ui-ux-pro-max 综合产出）

**风格定位**：冷纸白底 + 近黑文字 + 靛蓝智能强调的瑞士极简风——`ui-ux-pro-max` 对 Productivity Tool 的主推（Minimalism & Swiss Style）与用户「亮色极简专业」一致。刻意避开三种 AI 模板 look：无奶油底（#F4F1EA）、无 serif+terracotta、无近黑+酸性绿；无报纸式密集栏。底色用**冷调**纸白而非暖奶油，是本次最关键的差异化笔触。

**Design tokens（亮色）：**

| Token | 值 | 用途 |
|-------|-----|------|
| `--bg` | `#FAFAF8` | 页面底色（冷纸白） |
| `--fg` | `#1B1E24` | 主文字（近黑冷调） |
| `--muted` | `#6B7280` | 次级文字/时间戳 |
| `--border` | `#E4E6EB` | hairline 分隔 |
| `--primary` | `#4F46E5` | 靛蓝强调（思考/管线/焦点） |
| `--success` | `#0D9488` | 工具完成语义 |
| `--danger` | `#DC2626` | 错误语义 |

对比度：`--fg` 对 `--bg` ≈ 14:1（AAA）；`--muted` ≈ 5.4:1（AA）；`--primary` 白字对 `--primary` 底 ≈ 5:1（AA）。

**字体**（`ui-ux-pro-max` 的 `Tech Startup` / `Developer Mono` 配对演进，避免 Inter 模板感）：

| 角色 | 字体 | 理由 |
|------|------|------|
| Display | **Space Grotesk** | 几何感、开发者工具气质，用于品牌/标题，有节制 |
| Body | **DM Sans** | 干净现代，正文与 UI 文字 |
| Mono | **IBM Plex Mono** | 思考过程/管线/路径等「数据」文本，呼应思考可见定位 |

**布局**：中央单列消息流（max-width 760px，行宽 60-75 字符）；顶部细 header（hairline 下边线）；底部 sticky 输入区；用户消息右对齐浅 tint（`--primary` 8% 淡色），Agent 消息左对齐全宽；无气泡、hairline 分隔（瑞士风格），思考区缩进 + 靛蓝左边框。

**动效**（150-300ms，`prefers-reduced-motion` 时全关）：发送→管线节点依序浮现（stagger 30-50ms）→当前节点脉冲→完成 reveal。仅此一处编排，其余静止。

### D8. 构建集成与静态资源

- 前端 `vite.config.ts`：`base: '/api/'`（与 `context-path=/api` 对齐，否则构建产物资源路径 404）；dev 模式 `server.proxy['/api'] → http://localhost:8080`
- 后端 `pom.xml`：`maven-resources-plugin` 在 `process-resources` 阶段将 `web/dist` 复制进 `target/classes/static/`；`npm ci && npm run build` 由 `frontend-maven-plugin`（或预构建约定）执行——**设计倾向：前端构建由本机 node 完成，Maven 仅复制 dist**，避免 Maven 内嵌 node 下载（网络依赖），README 补充构建说明
- 访问入口：`http://localhost:8080/api/`（静态首页与 API 同源，无 CORS）

### D9. 真·流式打字（token 事件）

- 后端：`ToolCallAgent.think()` 由同步 `call()` 改为 `ChatClient.stream()`——`Flux<ChatResponse>` 逐段触发 `onToken`（`BaseAgent`/`GatewayMessage` 新增字段，null 时不触发、飞书行为不变），同时合并全部分片重建标准响应结构（`AssistantMessage.builder().content(...).toolCalls(...)` + `ChatResponse(Generation)`）喂给 `ToolCallingManager`，ReAct 循环与上下文管理逻辑不变
- SSE 新增 `token` 事件（增量文本）；`done` 的 content 在 `route()` 返回空时回退为最后一次 think 文本（Agent 的最终回复本就在最后一次 think 产生）
- 前端：`token` 事件追加到消息 `content`；`pendingReset` 机制——收到 `thought` 后下一批 `token` 先清空预览（中间思考/工具调用说明不混入最终回复）；打字机 interval 从 `content` 头部逐字追赶（content 驱动），`done` 后打完全文即停；等待期闪烁光标 + 点击跳过 + `prefers-reduced-motion` 直显
- 备选：EventSource/轮询（弃，EventSource 仅 GET）；仅最终 think 流式（无法预知「最后」，故每次 think 都推 token + 前端清理）

## Risks / Trade-offs

- **[共享上下文] web 与飞书对话互相可见、互相可清空** → MVP 接受（spec 已定），界面与 README 明示；后续迭代做按 channel 隔离上下文
- **[串行锁] 并发吞吐受限** → 单 Agent 单用户场景无感；飞书现有并发踩状态问题同时被修复
- **[onThought 仅 think 阶段] act/observe 过程不实时、管线无结构化工具数据** → 管线基于 think 文本 + 工具名轻量匹配（渐进增强）；不改 `BaseAgent` 签名保证飞书行为不变；后续可引入结构化事件流
- **[内存 session] 应用重启后所有会话失效，需重新登录** → MVP 接受；可选扩展：持久化或固定时长续期
- **[fetch 流解析] 需自行处理断线与重连** → 断线时界面提示并可手动重发；`done`/`error` 前网络中断按未完成回合处理
- **[静态打包] jar 体积增大** → 增量约数百 KB，可接受
- **[SseEmitter 超时/客户端断开] 连接泄漏** → 统一 `onTimeout/onError/onCompletion` 兜底 + `finally complete()`

## Migration Plan

1. 配置：`application.yml` 增加 `web.enabled: true`、`web.access-password: ${WEB_ACCESS_PASSWORD}`；`.env`/README 记录环境变量
2. 前端：`cd web && npm ci && npm run build` → 产物落入 `web/dist`
3. 启动：`.\mvnw.cmd spring-boot:run` → 访问 `http://localhost:8080/api/` → 密码登录 → 聊天
4. 回滚：设 `web.enabled=false` 重启，全部 web 端点（含静态页路由）关闭，飞书通道不受影响；删除前端目录不影响后端编译
5. 开发期：Vite dev server（`npm run dev`）代理 `/api` 至本地 8080，前后端热更新

## Open Questions

无阻塞性未知项。可延后确定：session 有效期精确值、管线工具名匹配的容错正则、是否提供页面级错误重试（与 done/error 事件语义正交，不改变 specs/方案/任务拆分）。

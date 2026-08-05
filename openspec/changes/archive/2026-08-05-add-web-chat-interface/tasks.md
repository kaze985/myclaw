# tasks: add-web-chat-interface

## 1. 后端：配置、认证与 Web Channel

- [x] 1.1 新增 `gateway.web` 包：`WebProperties`（`web.enabled` 默认 false、`web.access-password`），`application.yml` 增加 `web.*` 配置段（环境变量 `WEB_ACCESS_PASSWORD` 注入）
- [x] 1.2 新增 `WebSessionRegistry`：内存会话表（`ConcurrentHashMap<sessionId, expiresAt>`），签发/校验/注销会话
- [x] 1.3 新增 `WebAuthController`：`POST /api/auth/login`（校验密码，成功签发 HttpOnly cookie：`SameSite=Lax`、`Path=/api`、Max-Age 7d；失败 401）、`POST /api/auth/logout`
- [x] 1.4 新增 `WebAuthInterceptor` + WebMvcConfigurer：拦截 `/api/chat/**`、`/api/files/**`，放行 `/api/auth/login`；无效/缺失 cookie 返回 401；`web.enabled=false` 时不注册端点
- [x] 1.5 新增 `ChatController`：`POST /api/chat`（SseEmitter 超时 10min + onTimeout/onError/onCompletion 兜底，组装 `GatewayMessage` 调 `AgentMessageRouter.route()`，`onThought` 推 `thought` 事件，成功后推 `done`、异常推 `error`，finally complete）；`POST /api/chat/new`（复用清空上下文逻辑）
- [x] 1.6 `AgentMessageRouter.route()` 加 `synchronized`，串行化 web/飞书消息执行
- [x] 1.7 新增 `ArtifactDownloadController`：`GET /api/files/**`，路径拼接 `${user.dir}/tmp/file` 后 `Path.normalize().startsWith` 沙盒校验（越界 403、不存在 404、非 GET 405），Content-Type 按扩展名映射，`Content-Disposition: attachment`
- [x] 1.8 后端单测：登录成功/失败、会话拦截 401、路径穿越 403、下载 404、`/api/chat/new` 清空上下文（参照既有 `SandboxedFileOpsToolTest` 风格）

## 2. 前端：Vue 3 聊天界面

- [x] 2.1 初始化 `web/` 工程：`package.json`（vue、marked、highlight.js、vite、@vitejs/plugin-vue、typescript）、`vite.config.ts`（`base: '/api/'`、dev proxy `/api → localhost:8080`）、`tsconfig.json`、`index.html`
- [x] 2.2 建立 design tokens（`styles/tokens.css`：`--bg #FAFAF8`、`--fg #1B1E24`、`--muted #6B7280`、`--border #E4E6EB`、`--primary #4F46E5`、`--success #0D9488`、`--danger #DC2626`）+ 全局基础样式（字体栈 Space Grotesk / DM Sans / IBM Plex Mono、hairline 分隔、响应式）
- [x] 2.3 `useAuth` composable + `LoginView.vue`：密码提交至 `/api/auth/login`，401 提示错误，登录成功进入聊天视图；收到 401 时自动回登录视图且保留本地历史
- [x] 2.4 `useChat` composable：`fetch` POST `/api/chat` + `ReadableStream` 解析 SSE 帧（`thought`/`done`/`error` 事件，断线提示）；`useLocalHistory`：消息历史读写 `localStorage`
- [x] 2.5 `ChatView.vue` 组件树：`HeaderBar`（品牌 + 新建会话）、`MessageList`、`UserMessage`、`AssistantMessage`、`Composer`（发送中禁用 + 进行状态）
- [x] 2.6 `ReActPipeline.vue` 签名元素：thought 步骤管线（逐条节点 + 30-50ms stagger + 当前节点脉冲）、内置 11 个工具名轻量识别渲染徽标、完成后折叠为「思考过程」区
- [x] 2.7 `MarkdownBody.vue`：marked 渲染（安全转义）+ highlight.js 代码高亮
- [x] 2.8 产物链接识别：回复文本中 `tmp/file/xxx.ext` 路径渲染为 `GET /api/files/**` 下载链接（URL 编码中文文件名）
- [x] 2.9 可访问性与动效收尾：键盘焦点可见、`prefers-reduced-motion` 关闭动画、移动端适配、明示共享上下文提示文案

## 3. 构建集成与收尾

- [x] 3.1 `pom.xml` 增加 `maven-resources-plugin`：`process-resources` 阶段将 `web/dist` 复制进 `target/classes/static/`
- [ ] 3.2 前端构建后整体启动联调：`npm run build` → `.\mvnw.cmd spring-boot:run` → `http://localhost:8080/api/` 完整走通
- [x] 3.3 README 更新：`WEB_ACCESS_PASSWORD` 配置、`web.enabled`、前端构建步骤、web 与飞书共享上下文的说明
- [ ] 3.4 端到端验证：登录 → 聊天 → 思考流实时呈现 → 文档产物下载 → 新建会话；飞书通道回归（不受影响、并发消息排队）

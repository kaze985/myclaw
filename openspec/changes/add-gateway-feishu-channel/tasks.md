## 1. 依赖与基础结构

- [x] 1.1 在 `pom.xml` 中添加飞书 oapi-sdk 依赖（`com.larksuite.oapi:oapi-sdk:2.5.3`）
- [x] 1.2 创建包结构：`com.lppnb.ai.myclaw.gateway.channel`、`com.lppnb.ai.myclaw.gateway.feishu`

## 2. Channel 抽象层

- [x] 2.1 创建 `GatewayMessage.java`：统一消息模型，字段包含 `platform`、`sessionId`、`senderId`、`content`、`rawPayload`
- [x] 2.2 创建 `Channel.java` 接口：定义 `start()`、`stop()`、`send(GatewayMessage)` 三个方法
- [x] 2.3 创建 `MessageRouter.java` 接口：定义 `route(GatewayMessage): String` 方法

## 3. MessageRouter 默认实现

- [x] 3.1 创建 `AgentMessageRouter.java`：注入 `MyClaw` Agent，实现 `route()` 方法，调用 Agent 处理消息并返回回复内容
- [x] 3.2 在 `AgentMessageRouter.java` 中处理 Agent 异常，捕获后记录日志并返回错误提示文本

## 4. 飞书 Channel 配置

- [x] 4.1 创建 `FeishuProperties.java`（`@ConfigurationProperties(prefix = "gateway.feishu")`）：字段 `appId`、`appSecret`、`enabled`（默认 true）
- [x] 4.2 在 `application.yml` 中添加飞书配置占位符（`gateway.feishu.app-id`、`gateway.feishu.app-secret`、`gateway.feishu.enabled`）

## 5. 飞书事件处理器

- [x] 5.1 创建 `FeishuEventHandler.java`：构建 `EventDispatcher`，注册 `onP2MessageReceiveV1` 处理器
- [x] 5.2 在 `onP2MessageReceiveV1` 处理器中：提取消息内容（文本类型），构建 `GatewayMessage`，通过 `CompletableFuture.runAsync()` 异步调用 `MessageRouter.route()`
- [x] 5.3 在异步任务中调用飞书消息回复 API（`client.im().message().reply()`）将 Agent 回复发送至原会话
- [x] 5.5 异步任务执行超时或发生异常时，通过飞书消息回复 API 向用户发送错误提示文本（包含简要错误说明），方便用户排查
- [x] 5.4 对非文本消息类型（image、file 等）跳过处理并记录 DEBUG 日志

## 6. 飞书 Channel 实现

- [x] 6.1 创建 `FeishuChannel.java`（实现 `Channel`）：注入 `FeishuProperties` 和 `FeishuEventHandler`
- [x] 6.2 实现 `start()` 方法：构建飞书 `Client`（`com.lark.oapi.Client`）和 `ws.Client`，在守护线程中调用 `wsClient.start()`
- [x] 6.3 实现 `stop()` 方法：优雅关闭 WebSocket 连接
- [x] 6.4 实现 `send(GatewayMessage)` 方法：调用飞书消息 API 发送文本消息

## 7. Spring 自动配置

- [x] 7.1 创建 `FeishuConfig.java`（`@Configuration`）：在 `gateway.feishu.enabled=true` 时注册 `FeishuChannel` 和 `FeishuEventHandler` Bean
- [x] 7.2 创建 `GatewayAutoConfiguration.java`：注册 `AgentMessageRouter` Bean，并在应用启动后（`ApplicationRunner`）调用所有 `Channel.start()`
- [x] 7.3 确保 `@ConditionalOnProperty(prefix = "gateway.feishu", name = "enabled", havingValue = "true", matchIfMissing = true)` 条件正确控制 Bean 初始化

## 8. 验证与测试

- [ ] 8.1 启动应用，验证飞书 WebSocket 长连接建立成功（日志中出现 `connected to wss://...`）
- [ ] 8.2 通过飞书向机器人发送文本消息，验证 Agent 能够接收并回复
- [ ] 8.3 验证 `gateway.feishu.enabled=false` 时飞书 Channel 不启动
<!-- 8.1-8.3 为运行时验证任务，需配置真实飞书凭证后手动执行 -->

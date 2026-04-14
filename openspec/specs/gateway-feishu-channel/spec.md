### Requirement: 飞书 Channel 配置
系统 SHALL 支持通过 `application.yml` 配置飞书 Channel，配置项包括：`gateway.feishu.app-id`、`gateway.feishu.app-secret`、`gateway.feishu.enabled`（默认 true）。当 `gateway.feishu.enabled=false` 时，飞书 Channel SHALL 不启动。

#### Scenario: 配置合法时正常启动
- **WHEN** 应用启动且 `gateway.feishu.app-id` 和 `gateway.feishu.app-secret` 均已配置
- **THEN** 飞书 Channel SHALL 成功启动长连接，日志 SHALL 打印连接成功信息

#### Scenario: 配置缺失时启动失败
- **WHEN** 应用启动但 `gateway.feishu.app-id` 或 `gateway.feishu.app-secret` 未配置
- **THEN** 应用 SHALL 启动失败并输出明确的配置缺失错误信息

#### Scenario: 禁用飞书 Channel
- **WHEN** `gateway.feishu.enabled=false` 时
- **THEN** 飞书 Channel SHALL 不创建 WebSocket 连接，相关 Bean 不初始化

### Requirement: 长连接模式接收飞书消息
系统 SHALL 使用飞书 Java SDK 的 `com.lark.oapi.ws.Client` 建立 WebSocket 长连接，接收飞书机器人消息事件（`im.message.receive_v1`）。

#### Scenario: 收到文本消息事件
- **WHEN** 飞书用户向机器人发送文本消息时
- **THEN** 系统 SHALL 在 3 秒内 ACK 该事件，并异步将消息内容路由至 Agent 处理

#### Scenario: 长连接断开自动重连
- **WHEN** WebSocket 连接因网络原因断开时
- **THEN** 飞书 SDK 内置重连机制 SHALL 自动重新建立连接

#### Scenario: 收到非文本消息
- **WHEN** 飞书用户发送图片、文件等非文本消息时
- **THEN** 系统 SHALL 跳过该消息并记录 DEBUG 日志，不 SHALL 向用户回复错误

### Requirement: 飞书消息回复
系统 SHALL 使用飞书 Java SDK 的消息 API（`client.im().message().reply()`）将 Agent 回复以文本格式发送至原消息会话。

#### Scenario: Agent 回复成功发送
- **WHEN** Agent 处理完成并返回文本回复时
- **THEN** 系统 SHALL 调用飞书消息回复 API，将回复内容发送至对应会话，消息类型为 `text`

#### Scenario: 飞书 API 调用失败
- **WHEN** 飞书消息回复 API 返回非成功状态码时
- **THEN** 系统 SHALL 记录包含错误码和请求 ID 的 ERROR 日志，不 SHALL 重试（避免重复消息）

### Requirement: 异步消息处理
系统 SHALL 在飞书事件回调中立即返回（不阻塞 SDK 回调线程），通过 `CompletableFuture` 或 Spring 异步任务异步执行 Agent 调用，避免触发飞书的 3 秒超时重推机制。

#### Scenario: 消息处理超时不触发重推
- **WHEN** Agent 处理时间超过 3 秒时
- **THEN** 飞书事件 SHALL 已在 3 秒内得到 ACK，不 SHALL 触发重推；Agent 回复完成后通过消息 API 主动发送

#### Scenario: Agent 处理超时或异常时通知用户
- **WHEN** 异步 Agent 任务发生超时或运行时异常时
- **THEN** 系统 SHALL 通过飞书消息回复 API 向原会话发送错误提示文本（包含简要错误说明），帮助用户排查问题

#### Scenario: Agent 思考内容实时推送至飞书
- **WHEN** Agent `think` 阶段每次产生文本输出时
- **THEN** 系统 SHALL 立即通过飞书消息回复 API 将该思考文本发送至原会话，用户无需等待 Agent 全部执行完毕即可看到推理过程

#### Scenario: 并发消息处理
- **WHEN** 短时间内收到多条飞书消息时
- **THEN** 每条消息 SHALL 独立异步处理，互不阻塞

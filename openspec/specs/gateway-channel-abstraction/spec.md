### Requirement: Channel 接口定义
系统 SHALL 提供 `Channel` 接口，所有通讯平台集成 SHALL 实现该接口，实现可插拔的通道扩展能力。Channel 接口 SHALL 包含：启动通道（`start`）、停止通道（`stop`）、发送消息（`send`）三个方法。

#### Scenario: 启动通道
- **WHEN** Spring 应用上下文就绪后
- **THEN** 所有已配置的 Channel 实现 SHALL 自动启动并建立与平台的连接

#### Scenario: 停止通道
- **WHEN** Spring 应用关闭时
- **THEN** 所有已启动的 Channel SHALL 优雅关闭连接，释放资源

#### Scenario: 发送消息
- **WHEN** 系统调用 `Channel.send(GatewayMessage)` 时
- **THEN** Channel 实现 SHALL 将消息发送至对应平台的目标会话

### Requirement: 统一消息模型 GatewayMessage
系统 SHALL 定义 `GatewayMessage` 作为入站和出站消息的统一数据模型，包含：`platform`（平台标识）、`sessionId`（会话 ID）、`senderId`（发送者 ID）、`content`（消息文本内容）、`rawPayload`（原始平台消息对象，可空）、`onThought`（Agent 思考内容实时回调，可空）。

#### Scenario: 入站消息转换
- **WHEN** 收到来自任意平台的消息事件时
- **THEN** Channel 实现 SHALL 将平台原始消息转换为 `GatewayMessage`，所有必填字段 SHALL 非空

#### Scenario: 出站消息构建
- **WHEN** Agent 处理完成并需要回复时
- **THEN** 系统 SHALL 通过 `GatewayMessage` 携带 sessionId、content 等信息，调用 Channel.send() 发送回复

### Requirement: Agent 思考内容实时推送
系统 SHALL 支持通过 `GatewayMessage.onThought` 回调，将 Agent 每次 `think` 阶段产生的文本内容实时推送至调用方（如通讯平台客户端）。

#### Scenario: 实时推送思考内容
- **WHEN** `GatewayMessage.onThought` 不为 null，且 Agent `think` 阶段产生文本输出时
- **THEN** 系统 SHALL 立即调用 `onThought` 回调，将思考文本传递给调用方，无需等待整个 Agent 执行完毕

#### Scenario: 回调为 null 时正常运行
- **WHEN** `GatewayMessage.onThought` 为 null 时
- **THEN** 系统 SHALL 正常执行 Agent，不触发任何回调，行为与无回调场景一致

### Requirement: MessageRouter 消息路由接口
系统 SHALL 提供 `MessageRouter` 接口，将入站的 `GatewayMessage` 路由至 AI Agent 处理，并返回 Agent 回复内容。

#### Scenario: 路由消息至 Agent
- **WHEN** Channel 收到用户消息并转换为 `GatewayMessage` 后
- **THEN** Channel 实现 SHALL 调用 `MessageRouter.route(GatewayMessage)` 获取 Agent 回复

#### Scenario: Agent 处理异常
- **WHEN** `MessageRouter.route()` 执行过程中发生异常时
- **THEN** 系统 SHALL 捕获异常并记录日志，返回错误提示消息，不 SHALL 导致应用崩溃

## Why

当前 MyClaw 系统缺乏与外部通讯平台对接的统一入口，AI Agent 能力无法直接服务于企业即时通讯场景。通过引入可插拔的 Gateway 组件，可将飞书等平台的消息转化为统一内部事件，让 Agent 具备与真实用户对话的能力，从而拓展系统的实际应用价值。

## What Changes

- 新增 `com.lppnb.ai.myclaw.gateway` 顶层包，作为所有通道集成的统一入口
- 新增 **Channel 抽象层**（`gateway.channel` 包），定义通道插件化接口：
  - `Channel`：通道生命周期接口（`start`、`stop`、`send`）
  - `GatewayMessage`：统一入站/出站消息模型（`platform`、`sessionId`、`senderId`、`content`、`rawPayload`、`onThought`）
  - `MessageRouter`：消息路由接口，将入站消息交由 Agent 处理
  - `AgentMessageRouter`：`MessageRouter` 默认实现，注入 `MyClaw` Agent，每次路由前重置 Agent 状态
- 新增 **飞书 Channel 实现**（`gateway.feishu` 包），集成飞书 Java SDK（`oapi-sdk 2.5.3`），支持：
  - 长连接（WebSocket）模式接收飞书消息事件（`im.message.receive_v1`）
  - 接收到的消息转换为 `GatewayMessage` 并通过 `CompletableFuture` 异步路由至 Agent 处理
  - Agent 每次思考（`think` 阶段）产生的文本通过 `onThought` 回调实时回复至飞书会话
  - Agent 处理异常时向用户发送错误提示文本
  - 非文本消息类型（image、file 等）静默跳过
- 新增 **`BaseAgent.onThought` 回调机制**：`ToolCallAgent.think()` 每次产生文本时触发回调，供外部实时获取中间思考内容
- 新增飞书应用凭证配置项（`gateway.feishu.app-id`、`gateway.feishu.app-secret`、`gateway.feishu.enabled`）
- 新增 `pom.xml` 依赖：飞书 oapi-sdk

## Capabilities

### New Capabilities

- `gateway-channel-abstraction`: 定义通道抽象层，包含 Channel 接口、统一消息模型 GatewayMessage（含 `onThought` 实时回调字段）、消息路由接口及默认 Agent 实现
- `gateway-feishu-channel`: 飞书通道具体实现，负责与飞书开放平台建立 WebSocket 长连接、接收消息事件、将 Agent 中间思考实时回复至飞书会话

### Modified Capabilities

- `agent-core`（`BaseAgent` / `ToolCallAgent`）：新增 `onThought` 回调字段，`think()` 阶段产生文本时触发，支持外部实时订阅思考过程

## Impact

- **新增依赖**：`com.larksuite.oapi:oapi-sdk:2.5.3`（Maven pom.xml）
- **新增包**：`com.lppnb.ai.myclaw.gateway`（`channel`、`feishu` 子包）
- **新增配置**：`application.yml` 中飞书凭证及 Gateway 相关配置（通过环境变量 `FEISHU_APP_ID` / `FEISHU_APP_SECRET` 注入）
- **修改 Agent 核心**：`BaseAgent` 新增 `onThought` 和 `reply` 字段；`ToolCallAgent.think()` 触发 `onThought` 回调
- **无 Breaking 变更**：现有 Agent、Tool、Controller 代码不受影响；`onThought` 为 null 时不触发回调

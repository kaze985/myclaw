## Why

当前 MyClaw 系统缺乏与外部通讯平台对接的统一入口，AI Agent 能力无法直接服务于企业即时通讯场景。通过引入可插拔的 Gateway 组件，可将飞书等平台的消息转化为统一内部事件，让 Agent 具备与真实用户对话的能力，从而拓展系统的实际应用价值。

## What Changes

- 新增 `com.lppnb.ai.myclaw.gateway` 顶层包，作为所有通道集成的统一入口
- 新增 **Channel 抽象层**，定义通道插件化接口（`Channel`、`InboundMessage`、`OutboundMessage`）
- 新增 **飞书 Channel 实现**，集成飞书 Java SDK（`oapi-sdk 2.5.3`），支持：
  - 长连接（WebSocket）模式接收飞书消息事件
  - Webhook HTTP 模式作为备选（集成 SpringBoot Servlet 容器）
  - 接收到的消息转换为内部统一事件并路由至 Agent 处理
  - Agent 处理结果回复消息至飞书会话
- 新增飞书应用凭证配置项（`gateway.feishu.app-id`、`gateway.feishu.app-secret` 等）
- 新增 `pom.xml` 依赖：飞书 oapi-sdk

## Capabilities

### New Capabilities

- `gateway-channel-abstraction`: 定义通道抽象层，包含 Channel 接口、统一入站/出站消息模型、消息路由到 Agent 的接口
- `gateway-feishu-channel`: 飞书通道具体实现，负责与飞书开放平台建立连接、接收消息事件、回复消息

### Modified Capabilities

## Impact

- **新增依赖**：`com.larksuite.oapi:oapi-sdk:2.5.3`（Maven pom.xml）
- **新增包**：`com.lppnb.ai.myclaw.gateway`（channel、feishu 子包）
- **新增配置**：`application.yml` 中飞书凭证及 Gateway 相关配置
- **无 Breaking 变更**：现有 Agent、Tool 代码不受影响

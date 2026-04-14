## Context

MyClaw 是一个基于 Spring Boot 3 + Spring AI Alibaba 构建的 AI Agent 框架，当前具备工具调用能力但缺乏外部通讯平台集成。引入 Gateway 组件的目标是为 Agent 提供统一的消息入口，首期接入飞书（Lark）平台。

飞书 Java SDK（`oapi-sdk 2.5.3`）提供两种事件接收模式：
1. **长连接（WebSocket）**：SDK 内置，无需公网 IP，本地开发友好
2. **Webhook HTTP**：传统模式，需要公网可访问的 URL

项目使用 JDK 21、Spring Boot 3.5.x、Lombok，包路径为 `com.lppnb.ai.myclaw`。

## Goals / Non-Goals

**Goals:**
- 定义可插拔的 Channel 抽象层，支持未来接入微信、QQ 等平台
- 实现飞书 Channel，支持长连接模式接收消息事件并回复
- 将飞书消息转换为内部统一 `GatewayMessage` 模型，交由 MyClaw Agent 处理
- 通过 Spring Boot 配置（`application.yml`）管理飞书凭证

**Non-Goals:**
- 暂不实现微信、QQ 等其他平台 Channel
- 暂不实现消息持久化、会话历史管理
- 暂不实现卡片消息、多媒体消息的完整渲染（仅处理文本消息）
- 暂不支持飞书机器人主动推送（非事件触发场景）

## Decisions

### 决策 1：长连接（WebSocket）为默认模式

**选择**：优先使用飞书 SDK 内置长连接（`com.lark.oapi.ws.Client`），Webhook 模式作为可选配置。

**理由**：
- 长连接无需公网 IP，开发测试阶段成本极低（官方称从 1 周降至 5 分钟）
- SDK 内置鉴权与自动重连，无需自行处理加解密
- 与 Spring Boot 生命周期集成：通过 `ApplicationRunner` 在应用启动后在独立线程中启动 `ws.Client`

**备选**：Webhook 模式需要配置 `VerificationToken`、`EncryptKey`，并暴露公网端点，复杂度更高，后续可通过 profile 切换支持。

### 决策 2：Channel 抽象设计

**选择**：定义 `Channel` 接口 + `GatewayMessage` 统一消息模型。

```
gateway/
  channel/
    Channel.java            # 通道接口
    GatewayMessage.java     # 统一消息模型（入站/出站共用）
    MessageRouter.java      # 路由器接口，将消息交由 Agent 处理
  feishu/
    FeishuChannel.java      # 飞书 Channel 实现（实现 Channel）
    FeishuEventHandler.java # 飞书事件处理（封装 EventDispatcher）
    FeishuProperties.java   # 配置属性
    FeishuConfig.java       # Spring @Configuration，装配 Client/Channel
  GatewayAutoConfiguration.java  # 整体自动配置
```

**理由**：
- 接口隔离，各平台 Channel 只需实现统一接口，后续扩展只需增加新包
- `GatewayMessage` 统一字段（senderId、sessionId、content、platform），Agent 处理层与具体平台解耦

### 决策 3：Agent 调用方式

**选择**：通过注入 `MyClaw`（现有 Agent）处理消息，并将结果以文本消息形式回复至飞书。

**理由**：
- `MyClaw` 已实现 `ToolCallAgent`，具备工具调用能力，可直接复用
- 消息处理在飞书 SDK 回调线程中同步执行（需在 3 秒内响应，否则触发超时重推），长耗时任务应异步处理并通过 API 主动回复

**约束**：飞书要求消息处理在 3 秒内完成（否则触发重推机制），因此事件处理器应立即 ACK 并异步调用 Agent，避免阻塞。

### 决策 4：Maven 依赖版本

**选择**：使用 `com.larksuite.oapi:oapi-sdk:2.5.3`（官方最新稳定版）。

不引入 `oapi-sdk-servlet-ext`，因为默认使用长连接模式，无需 Servlet 扩展包。

## Risks / Trade-offs

- **长连接阻塞主线程风险** → 通过独立守护线程启动 `ws.Client`，不阻塞 Spring Boot 主线程
- **飞书事件 3 秒超时重推** → 事件处理器立即返回，Agent 调用通过 `CompletableFuture` 异步执行，结果通过飞书消息 API 回复
- **多实例部署下消息只送达一个实例** → 飞书长连接为集群模式（官方说明），同一应用多个客户端只有一个收到消息；当前单实例场景无影响，后续如需多实例需引入消息队列
- **凭证安全** → `app-id`/`app-secret` 通过 `application.yml` 配置，生产环境应注入环境变量或 Secret 管理服务

## Open Questions

~~- 是否需要支持多租户（多个飞书应用实例）？~~ **已决策**：不需要，当前设计为单应用，不支持多租户。

~~- Agent 调用超时后，是否需要向用户发送"处理中"提示消息？~~ **已决策**：超时或处理异常后，向用户发送错误信息（包含简要错误说明），方便用户排查问题。

# web-chat-channel Specification

## Purpose

为 MyClaw 提供浏览器直连的 Web 聊天通道：通过 SSE 流式接收 Agent 的实时思考、增量文本与最终回复，与现有飞书通道共用同一 Agent 实例与全局上下文，扩展 Agent 的交互入口。

## Requirements

### Requirement: Web 通道配置

系统 SHALL 支持通过 `application.yml` 配置 Web 通道：`web.enabled`（默认 false）、`web.access-password`（通过环境变量 `WEB_ACCESS_PASSWORD` 注入）。当 `web.enabled=false` 或访问密码未配置时，Web 通道 SHALL 不提供聊天与认证端点。

#### Scenario: 启用 Web 通道

- **WHEN** `web.enabled=true` 且 `WEB_ACCESS_PASSWORD` 已配置
- **THEN** 系统 SHALL 启动 Web 聊天端点，未登录请求 SHALL 被拒绝并返回 401

#### Scenario: 未启用 Web 通道

- **WHEN** `web.enabled=false`（默认）
- **THEN** 系统 SHALL 不注册任何 Web 聊天或认证端点，不影响飞书通道运行

### Requirement: SSE 流式聊天

系统 SHALL 提供 `POST /api/chat` 端点，以 SSE 流（`text/event-stream`）响应。Agent 每次思考产生文本时 SHALL 通过 `onThought` 回调立即推送 `thought` 事件；Agent 执行完成后 SHALL 推送包含最终回复的 `done` 事件并关闭流；发生异常时 SHALL 推送 `error` 事件并关闭流。

#### Scenario: 正常聊天流程

- **WHEN** 已认证用户向 `POST /api/chat` 提交消息文本
- **THEN** 系统 SHALL 立即返回 SSE 流，按产生顺序推送 `thought` 事件（思考/工具调用过程），最终推送 `done` 事件携带完整回复并关闭流

#### Scenario: Agent 执行异常

- **WHEN** Agent 处理消息过程中抛出异常
- **THEN** 系统 SHALL 推送 `error` 事件（携带错误说明）并关闭流，不 SHALL 挂起连接

#### Scenario: 未认证请求

- **WHEN** 未携带有效会话 cookie 的请求访问 `POST /api/chat`
- **THEN** 系统 SHALL 返回 401，不 SHALL 执行 Agent

### Requirement: 流式增量文本事件（token）

系统 SHALL 在模型流式输出时通过 SSE 推送 `token` 事件（携带增量文本），供前端实现真·流式打字效果；`done` 事件 SHALL 携带最终回复完整文本，Agent 执行无显式返回时 SHALL 回退为最后一次思考（think）文本。

#### Scenario: 模型流式输出增量文本

- **WHEN** Agent 思考阶段模型流式生成文本时
- **THEN** 系统 SHALL 逐段推送 `token` 事件，前端可实时追加显示

#### Scenario: 最终回复内容回退

- **WHEN** `done` 事件发送且 Agent 执行无显式返回文本时
- **THEN** 系统 SHALL 以最后一次 think 文本作为最终回复内容推送

### Requirement: 会话控制

系统 SHALL 提供 `POST /api/chat/new` 端点，清空 Agent 全局上下文并重置运行状态（等价于飞书 `/new` 命令），返回成功提示。

#### Scenario: 新建会话

- **WHEN** 已认证用户调用 `POST /api/chat/new`
- **THEN** 系统 SHALL 清空 Agent 上下文、重置步数计数与运行状态，返回成功提示文本

### Requirement: 共享上下文与串行执行

Web 通道 SHALL 与飞书通道共用同一 Agent 实例与全局上下文。Agent 执行 SHALL 串行化：同一时刻仅处理一条消息，Web 与飞书消息并发到达时 SHALL 排队等待，避免共享状态被并发破坏。

#### Scenario: Web 与飞书消息并发到达

- **WHEN** Web 聊天请求与飞书消息同时触发 Agent 执行
- **THEN** 两条消息 SHALL 串行处理，后到消息等待前一条完成后执行，上下文 SHALL 保持完整不互相污染

#### Scenario: 连续多轮对话

- **WHEN** 已认证用户在 Web 通道连续发送多条消息
- **THEN** 系统 SHALL 保留上下文历史，后续消息基于前文多轮连续作答

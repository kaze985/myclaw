## Purpose

提供 Vue 3 单页聊天界面：以亮色极简的视觉呈现 Agent 对话，实时展示思考与工具调用过程，支持 Markdown 渲染、代码高亮、本地历史持久化与产物下载，并通过登录门接入后端认证。

## ADDED Requirements

### Requirement: 登录门

聊天界面 SHALL 在未认证时展示登录视图：用户输入访问密码，提交至 `POST /api/auth/login`；登录成功 SHALL 进入聊天视图，登录失败 SHALL 在界面内提示错误。

#### Scenario: 首次访问

- **WHEN** 未认证用户打开页面
- **THEN** 界面 SHALL 仅显示登录视图，不 SHALL 展示聊天内容

#### Scenario: 登录失败

- **WHEN** 用户提交错误密码且后端返回 401
- **THEN** 界面 SHALL 显示密码错误提示，停留于登录视图

#### Scenario: 会话失效重登

- **WHEN** 已登录用户发起请求但收到 401（会话过期/被登出）
- **THEN** 界面 SHALL 自动回到登录视图并保留本地聊天历史

### Requirement: 消息流与流式呈现

聊天界面 SHALL 展示用户消息与 Agent 回复的消息流，并通过 SSE 订阅 `POST /api/chat`：`thought` 事件 SHALL 实时追加为进行中的过程消息，`done` 事件 SHALL 以最终回复替换/完成当前回合，`error` 事件 SHALL 在界面内展示错误。

#### Scenario: 发送消息并流式接收

- **WHEN** 用户输入消息并点击发送
- **THEN** 界面 SHALL 立即显示用户消息，随后实时追加 Agent 思考/工具调用过程，最终显示完整回复；发送期间输入框 SHALL 禁用并显示进行状态

#### Scenario: 流式过程展示

- **WHEN** Agent 产生多条 `thought` 事件
- **THEN** 界面 SHALL 按到达顺序实时展示每条思考内容，无需等待 Agent 全部完成

### Requirement: Markdown 渲染与代码高亮

Agent 的最终回复 SHALL 按 Markdown 渲染（标题、列表、表格、链接等），代码块 SHALL 支持语法高亮；渲染 SHALL 使用安全的转义策略，不 SHALL 执行任何内嵌脚本。

#### Scenario: 回复含代码块

- **WHEN** Agent 回复包含 Markdown 代码块
- **THEN** 界面 SHALL 高亮显示代码并保持可复制，不 SHALL 执行其中内容

### Requirement: 产物下载链接

界面 SHALL 识别 Agent 回复中指向工具产物文件的路径（位于 `tmp/file` 下的文档路径），将其渲染为可下载链接，指向 `GET /api/files/**` 端点。

#### Scenario: 回复包含文档路径

- **WHEN** Agent 回复包含如 `tmp/file/xxx.pdf` 的产物路径
- **THEN** 界面 SHALL 将该路径渲染为可点击下载链接，点击后下载对应文件

### Requirement: 会话控制与历史持久化

界面 SHALL 提供「新建会话」按钮：点击后 SHALL 清空本地消息历史并调用 `POST /api/chat/new` 清空后端上下文。消息历史 SHALL 持久化至浏览器 `localStorage`，页面刷新后 SHALL 恢复展示。

#### Scenario: 刷新页面保留历史

- **WHEN** 用户刷新页面
- **THEN** 界面 SHALL 从 `localStorage` 恢复之前的消息流与进行状态

#### Scenario: 新建会话

- **WHEN** 用户点击「新建会话」
- **THEN** 界面 SHALL 清空本地历史、调用后端清空上下文，并展示空会话欢迎态

### Requirement: 可用性与可访问性

界面 SHALL 遵循亮色极简的专业视觉基调，适配移动端与桌面端；交互元素 SHALL 提供可见键盘焦点，动画 SHALL 尊重 `prefers-reduced-motion`。

#### Scenario: 键盘操作

- **WHEN** 用户仅使用键盘操作界面
- **THEN** 所有交互元素（发送、新建、登录、下载链接）SHALL 可聚焦可触发，焦点 SHALL 清晰可见

#### Scenario: 减少动态效果

- **WHEN** 系统开启 `prefers-reduced-motion`
- **THEN** 界面 SHALL 关闭或最小化非必要动画

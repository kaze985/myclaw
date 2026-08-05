## Purpose

为 Web 通道提供轻量访问认证：基于单一访问密码登录并签发 HttpOnly cookie 会话，所有 Web 端点经拦截器校验，防止未授权用户驱动 Agent 的终端与文件工具能力。

## ADDED Requirements

### Requirement: 密码登录

系统 SHALL 提供 `POST /api/auth/login` 端点，校验请求携带的密码与环境变量 `WEB_ACCESS_PASSWORD` 是否一致。校验通过时 SHALL 创建服务端会话并签发 HttpOnly cookie；校验失败 SHALL 返回 401。

#### Scenario: 密码正确

- **WHEN** 用户提交与 `WEB_ACCESS_PASSWORD` 一致的密码
- **THEN** 系统 SHALL 返回成功响应并设置 HttpOnly cookie，后续请求携带该 cookie 即视为已认证

#### Scenario: 密码错误

- **WHEN** 用户提交错误密码
- **THEN** 系统 SHALL 返回 401，不 SHALL 签发任何会话

### Requirement: 会话校验拦截

系统 SHALL 拦截所有 Web 通道端点（聊天、会话控制、产物下载），校验请求携带的 HttpOnly cookie 是否为有效会话；无效或缺失时 SHALL 返回 401。

#### Scenario: 携带有效会话

- **WHEN** 请求携带服务端已签发的有效会话 cookie
- **THEN** 系统 SHALL 放行请求，正常处理聊天或下载

#### Scenario: 会话缺失或失效

- **WHEN** 请求未携带 cookie，或 cookie 对应会话已失效/不存在
- **THEN** 系统 SHALL 返回 401，不 SHALL 执行任何 Agent 或文件操作

### Requirement: 认证态探测

系统 SHALL 提供 `GET /api/auth/me` 端点，返回当前请求的会话是否有效（有效返回 200 与认证状态，无效返回 401），供前端页面加载时判断登录状态。

#### Scenario: 已登录探测

- **WHEN** 请求携带有效会话 cookie 访问 `GET /api/auth/me`
- **THEN** 系统 SHALL 返回 200 并携带认证状态

#### Scenario: 未登录探测

- **WHEN** 请求未携带或携带失效会话 cookie 访问 `GET /api/auth/me`
- **THEN** 系统 SHALL 返回 401

### Requirement: 登出

系统 SHALL 提供 `POST /api/auth/logout` 端点，使当前会话失效并清除客户端 cookie。

#### Scenario: 用户登出

- **WHEN** 已认证用户调用 `POST /api/auth/logout`
- **THEN** 系统 SHALL 注销服务端会话并清除 cookie，之后原 cookie 请求 SHALL 返回 401

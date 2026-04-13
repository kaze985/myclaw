## ADDED Requirements

### Requirement: Agent 可执行终端命令并获取输出
系统 SHALL 提供一个 `@Tool` 标注的工具方法，接受命令字符串，通过 `ProcessBuilder` 在本地终端执行命令，并返回命令的标准输出（stdout）和标准错误（stderr）合并后的字符串。

执行前 SHALL 进行命令白名单校验：提取命令字符串的首个词（程序名），在 `tools.terminal.allowed-commands` 配置的列表中查找；不在白名单的命令 SHALL 被直接拒绝，返回错误信息 "命令被拒绝：{程序名} 不在允许列表中"，不执行任何进程。

系统 SHALL 在运行时通过 `System.getProperty("os.name")` 检测操作系统：
- Windows：使用 `["cmd.exe", "/c", command]` 执行
- Linux/Mac：使用 `["/bin/sh", "-c", command]` 执行

命令执行超时 SHALL 设置为 30 秒；超时后进程被强制终止，并返回超时错误信息。

工具返回值 SHALL 包含退出码和输出内容，格式为：`[exitCode=N]\n{output}`

#### Scenario: 在 Windows 上执行白名单内命令
- **WHEN** 系统运行在 Windows 上，Agent 调用终端工具传入白名单内命令 `echo hello`
- **THEN** 工具使用 `cmd.exe /c` 执行命令，返回包含 `hello` 的输出字符串

#### Scenario: 在 Linux 上执行白名单内命令
- **WHEN** 系统运行在 Linux 上，Agent 调用终端工具传入白名单内命令 `echo hello`
- **THEN** 工具使用 `/bin/sh -c` 执行命令，返回包含 `hello` 的输出字符串

#### Scenario: 执行白名单外命令被拒绝
- **WHEN** Agent 传入不在白名单中的命令（如 `rm -rf /`）
- **THEN** 工具返回 "命令被拒绝：rm 不在允许列表中"，不启动任何进程

#### Scenario: 命令执行超时被强制终止
- **WHEN** Agent 传入一个执行时间超过 30 秒的白名单内命令
- **THEN** 进程在 30 秒后被强制终止，工具返回包含 "命令执行超时" 的错误信息

#### Scenario: 命令执行失败（非零退出码）
- **WHEN** Agent 传入一个会失败的白名单内命令
- **THEN** 工具返回包含非零退出码和 stderr 内容的字符串，不抛出未处理异常

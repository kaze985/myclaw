package com.lppnb.ai.myclaw.agent.tool.terminal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 终端命令执行工具：支持 Windows/Linux 双平台，命令白名单防护，30 秒超时。
 */
@Slf4j
@Component
public class TerminalExecuteTool {

    @Value("${tools.terminal.allowed-commands:echo,ls,dir,pwd,cat,grep,curl,java,mvn}")
    private String allowedCommandsConfig;

    @Tool(description = "Execute a shell command and return its output.")
    public String executeCommand(
            @ToolParam(description = "The shell command to execute") String command) {
        String trimmed = command.trim();
        if (trimmed.isEmpty()) {
            return "[exitCode=-1]\n命令为空";
        }

        String firstWord = trimmed.split("\\s+")[0];
        List<String> allowedCommands = Arrays.stream(allowedCommandsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (!allowedCommands.contains(firstWord)) {
            return "命令被拒绝：'" + firstWord + "' 不在允许的命令白名单中（白名单：" + allowedCommandsConfig + "）";
        }

        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            String[] cmdArray;
            if (os.contains("win")) {
                cmdArray = new String[]{"cmd.exe", "/c", command};
            } else {
                cmdArray = new String[]{"/bin/sh", "-c", command};
            }

            ProcessBuilder pb = new ProcessBuilder(cmdArray);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "[exitCode=-1]\n命令执行超时（30秒），已强制终止";
            }

            int exitCode = process.exitValue();
            log.debug("命令执行完毕：exitCode={}, command={}", exitCode, command);
            return "[exitCode=" + exitCode + "]\n" + output;

        } catch (Exception e) {
            log.error("命令执行异常：{}", command, e);
            return "[exitCode=-1]\n命令执行失败：" + e.getMessage();
        }
    }
}

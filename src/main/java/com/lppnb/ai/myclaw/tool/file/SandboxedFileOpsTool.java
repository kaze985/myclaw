package com.lppnb.ai.myclaw.tool.file;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 沙盒文件操作工具：所有操作严格限制在 user.dir 目录内，禁止删除操作。
 */
@Slf4j
@Component
public class SandboxedFileOpsTool {

    private static final String SANDBOX_ROOT = System.getProperty("user.dir");
    private static final String REJECT_MSG = "操作被拒绝：路径越界";

    /** 规范化路径并验证是否在沙盒范围内 */
    private boolean isPathSafe(String path) {
        try {
            Path resolved = Paths.get(path).toAbsolutePath().normalize();
            Path root = Paths.get(SANDBOX_ROOT).toAbsolutePath().normalize();
            return resolved.startsWith(root);
        } catch (Exception e) {
            return false;
        }
    }

    @Tool(description = "Read the text content of a file.")
    public String readFile(
            @ToolParam(description = "Path to the file") String path) {
        if (!isPathSafe(path)) {
            return REJECT_MSG;
        }
        try {
            return FileUtil.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "读取文件失败：" + e.getMessage();
        }
    }

    @Tool(description = "Write text content to a file.")
    public String writeFile(
            @ToolParam(description = "Path to the target file") String path,
            @ToolParam(description = "The text content to write") String content) {
        if (!isPathSafe(path)) {
            return REJECT_MSG;
        }
        try {
            FileUtil.writeString(content, path, StandardCharsets.UTF_8);
            return "文件写入成功：" + new File(path).getAbsolutePath();
        } catch (Exception e) {
            return "写入文件失败：" + e.getMessage();
        }
    }

    @Tool(description = "List the contents of a directory.")
    public String listDirectory(
            @ToolParam(description = "Path to the directory") String path) {
        if (!isPathSafe(path)) {
            return REJECT_MSG;
        }
        try {
            File dir = new File(path);
            if (!dir.exists()) {
                return "目录不存在：" + path;
            }
            if (!dir.isDirectory()) {
                return "不是目录：" + path;
            }
            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {
                return "目录为空";
            }
            return Arrays.stream(files)
                    .map(f -> (f.isDirectory() ? "[DIR]  " : "[FILE] ") + f.getName())
                    .sorted()
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "列出目录失败：" + e.getMessage();
        }
    }

    @Tool(description = "Create a directory.")
    public String createDirectory(
            @ToolParam(description = "Path of the directory to create") String path) {
        if (!isPathSafe(path)) {
            return REJECT_MSG;
        }
        try {
            FileUtil.mkdir(path);
            return "目录创建成功：" + new File(path).getAbsolutePath();
        } catch (Exception e) {
            return "创建目录失败：" + e.getMessage();
        }
    }
}

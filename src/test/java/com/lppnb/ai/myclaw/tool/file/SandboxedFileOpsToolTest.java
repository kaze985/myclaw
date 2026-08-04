package com.lppnb.ai.myclaw.tool.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * 沙盒文件操作安全测试：重点覆盖 `~` 展开绕过、路径穿越等越界场景。
 */
class SandboxedFileOpsToolTest {

    private final SandboxedFileOpsTool tool = new SandboxedFileOpsTool();

    private static final String REJECT_MSG = "操作被拒绝：路径越界";

    @Test
    void writeFileWithTildePathIsRejected() {
        String result = tool.writeFile(
                "~/.openclaw/skills/find-skills/SKILL.md",
                "---\nname: find-skills\n---");
        assertEquals(REJECT_MSG, result, "~ 开头的 home 路径必须被拒绝");
        // 确保项目下没有产生字面 ~ 目录（Java 不展开 ~，若校验失守会写到这里）
        File tildeDir = new File(System.getProperty("user.dir") + File.separator + "~");
        assertFalse(tildeDir.exists(), "user.dir 下不应产生 ~ 目录");
    }

    @Test
    void readFileWithTildePathIsRejected() {
        assertEquals(REJECT_MSG, tool.readFile("~/.openclaw/skills/find-skills/SKILL.md"));
    }

    @Test
    void listDirectoryWithTildePathIsRejected() {
        assertEquals(REJECT_MSG, tool.listDirectory("~/.openclaw"));
    }

    @Test
    void createDirectoryWithTildePathIsRejected() {
        assertEquals(REJECT_MSG, tool.createDirectory("~/evil"));
    }

    @Test
    void traversalOutsideSandboxIsRejected() {
        assertEquals(REJECT_MSG, tool.readFile("../../etc/passwd"));
        assertEquals(REJECT_MSG, tool.writeFile(".." + File.separator + "secret.txt", "x"));
    }

    @Test
    void relativePathInsideSandboxIsAllowed() {
        // 相对路径基于 user.dir 解析，应在沙盒内放行
        String path = "tmp" + File.separator + "sandbox-test-" + System.nanoTime() + ".txt";
        try {
            String result = tool.writeFile(path, "hello");
            assertTrue(result.startsWith("文件写入成功"), "沙盒内相对路径写入应成功: " + result);
            assertTrue(tool.readFile(path).contains("hello"));
        } finally {
            new File(path).delete();
        }
    }
}

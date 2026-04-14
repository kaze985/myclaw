package com.lppnb.ai.myclaw.tool;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.lppnb.ai.myclaw.tool.download.ResourceDownloadTool;
import com.lppnb.ai.myclaw.tool.excel.ExcelGenerateTool;
import com.lppnb.ai.myclaw.tool.file.SandboxedFileOpsTool;
import com.lppnb.ai.myclaw.tool.pdf.PdfGenerateTool;
import com.lppnb.ai.myclaw.tool.ppt.PptGenerateTool;
import com.lppnb.ai.myclaw.tool.scrape.WebScrapeTool;
import com.lppnb.ai.myclaw.tool.search.WebSearchTool;
import com.lppnb.ai.myclaw.tool.special.TerminateTool;
import com.lppnb.ai.myclaw.tool.terminal.TerminalExecuteTool;
import com.lppnb.ai.myclaw.tool.word.WordGenerateTool;

/**
 * 工具统一注册入口：将所有 Agent 工具通过 ToolCallbacks.from() 注册到框架。
 *
 * @author kaze
 * @date 2026/4/10 14:46
 */
@Configuration
public class ToolRegistration {

    private final WebSearchTool webSearchTool;
    private final WebScrapeTool webScrapeTool;
    private final ResourceDownloadTool resourceDownloadTool;
    private final TerminalExecuteTool terminalExecuteTool;
    private final SandboxedFileOpsTool sandboxedFileOpsTool;
    private final PdfGenerateTool pdfGenerateTool;
    private final WordGenerateTool wordGenerateTool;
    private final PptGenerateTool pptGenerateTool;
    private final ExcelGenerateTool excelGenerateTool;
    private final TerminateTool terminateTool;

    public ToolRegistration(WebSearchTool webSearchTool,
                            WebScrapeTool webScrapeTool,
                            ResourceDownloadTool resourceDownloadTool,
                            TerminalExecuteTool terminalExecuteTool,
                            SandboxedFileOpsTool sandboxedFileOpsTool,
                            PdfGenerateTool pdfGenerateTool,
                            WordGenerateTool wordGenerateTool,
                            PptGenerateTool pptGenerateTool,
                            ExcelGenerateTool excelGenerateTool,
                            TerminateTool terminateTool) {
        this.webSearchTool = webSearchTool;
        this.webScrapeTool = webScrapeTool;
        this.resourceDownloadTool = resourceDownloadTool;
        this.terminalExecuteTool = terminalExecuteTool;
        this.sandboxedFileOpsTool = sandboxedFileOpsTool;
        this.pdfGenerateTool = pdfGenerateTool;
        this.wordGenerateTool = wordGenerateTool;
        this.pptGenerateTool = pptGenerateTool;
        this.excelGenerateTool = excelGenerateTool;
        this.terminateTool = terminateTool;
    }

    @Bean
    public ToolCallback[] allTools() {
        return ToolCallbacks.from(
                terminateTool,
                webSearchTool,
                webScrapeTool,
                resourceDownloadTool,
                terminalExecuteTool,
                sandboxedFileOpsTool,
                pdfGenerateTool,
                wordGenerateTool,
                pptGenerateTool,
                excelGenerateTool
        );
    }
}

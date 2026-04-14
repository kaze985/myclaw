package com.lppnb.ai.myclaw.tool.pdf;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * PDF 生成工具：将 HTML 内容通过 Flying Saucer + OpenPDF 渲染为 PDF，支持中文字体。
 */
@Slf4j
@Component
public class PdfGenerateTool {

    @Value("${tools.file.download-dir}")
    private String downloadDir;

    /** 尝试注册的中文字体路径（Windows / Linux 常见路径） */
    private static final String[] CHINESE_FONT_PATHS = {
            "C:/Windows/Fonts/simsun.ttc",
            "C:/Windows/Fonts/msyh.ttc",
            "C:/Windows/Fonts/simhei.ttf",
            "/usr/share/fonts/truetype/arphic/uming.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc"
    };

    @PostConstruct
    public void init() {
        new File(downloadDir).mkdirs();
        log.info("PDF 输出目录就绪：{}", downloadDir);
    }

    @Tool(description = "Generate a PDF file from HTML content.")
    public String generatePdf(
            @ToolParam(description = "Output file name without extension") String filename,
            @ToolParam(description = "HTML content to render") String htmlContent) {
        try {
            File outFile = new File(downloadDir, filename + ".pdf");
            outFile.getParentFile().mkdirs();

            String document = wrapHtml(htmlContent);

            ITextRenderer renderer = new ITextRenderer();
            registerChineseFonts(renderer);
            renderer.setDocumentFromString(document);
            renderer.layout();

            try (OutputStream os = new FileOutputStream(outFile)) {
                renderer.createPDF(os);
            }

            log.info("PDF 生成成功：{}", outFile.getAbsolutePath());
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            log.error("PDF 生成失败：{}", filename, e);
            return "PDF生成失败：" + e.getMessage();
        }
    }

    /** 若传入的是片段，补全为合法的 XHTML 文档 */
    private String wrapHtml(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("<?xml") || trimmed.startsWith("<!DOCTYPE")
                || trimmed.toLowerCase().startsWith("<html")) {
            return content;
        }
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
                    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head>
                  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
                  <style>
                    body { font-family: SimSun, SimHei, Arial, sans-serif; font-size: 12pt; line-height: 1.6; }
                    h1, h2, h3 { font-weight: bold; }
                  </style>
                </head>
                <body>
                """ + content + """
                </body>
                </html>
                """;
    }

    private void registerChineseFonts(ITextRenderer renderer) {
        for (String fontPath : CHINESE_FONT_PATHS) {
            File fontFile = new File(fontPath);
            if (fontFile.exists()) {
                try {
                    renderer.getFontResolver().addFont(fontPath, "Identity-H", true);
                    log.debug("已注册中文字体：{}", fontPath);
                } catch (Exception e) {
                    log.warn("字体注册失败：{} -> {}", fontPath, e.getMessage());
                }
            }
        }
    }
}

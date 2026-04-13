package com.lppnb.ai.myclaw.agent.tool.word;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * Word 文档生成工具：按段落列表生成 .docx 文件，支持标题级别（1-3）和正文（0）。
 */
@Slf4j
@Component
public class WordGenerateTool {

    @Value("${tools.file.download-dir}")
    private String downloadDir;

    @PostConstruct
    public void init() {
        new File(downloadDir).mkdirs();
    }

    /**
     * 段落数据模型。
     * level: 0=正文，1=一级标题，2=二级标题，3=三级标题
     */
    @Data
    public static class Paragraph {
        private int level;
        private String text;
    }

    @Tool(description = "Generate a Word (.docx) document from a list of paragraphs.")
    public String generateWord(
            @ToolParam(description = "Output file name without extension") String filename,
            @ToolParam(description = "List of paragraphs, each with 'level' (0=body, 1-3=heading) and 'text'") List<Paragraph> paragraphs) {
        try {
            File outFile = new File(downloadDir, filename + ".docx");
            outFile.getParentFile().mkdirs();

            try (XWPFDocument doc = new XWPFDocument()) {
                for (Paragraph para : paragraphs) {
                    XWPFParagraph p = doc.createParagraph();
                    XWPFRun run = p.createRun();
                    run.setText(para.getText());

                    int level = para.getLevel();
                    if (level >= 1 && level <= 3) {
                        // 标题样式：级别越低字号越大
                        run.setBold(true);
                        run.setFontSize(24 - (level - 1) * 4);  // H1=24, H2=20, H3=16
                        p.setStyle("Heading" + level);
                    } else {
                        run.setFontSize(11);
                    }
                }

                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    doc.write(fos);
                }
            }

            log.info("Word 文档生成成功：{}", outFile.getAbsolutePath());
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            log.error("Word 文档生成失败：{}", filename, e);
            return "Word文档生成失败：" + e.getMessage();
        }
    }
}

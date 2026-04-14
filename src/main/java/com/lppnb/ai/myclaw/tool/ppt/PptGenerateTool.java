package com.lppnb.ai.myclaw.tool.ppt;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * PPT 演示文稿生成工具：使用 Apache POI 创建 .pptx 文件，每张幻灯片含标题和要点列表。
 */
@Slf4j
@Component
public class PptGenerateTool {

    @Value("${tools.file.download-dir}")
    private String downloadDir;

    @PostConstruct
    public void init() {
        new File(downloadDir).mkdirs();
    }

    /**
     * 幻灯片数据模型。
     */
    @Data
    public static class Slide {
        private String title;
        private List<String> bullets;
    }

    @Tool(description = "Generate a PowerPoint (.pptx) presentation from a list of slides.")
    public String generatePpt(
            @ToolParam(description = "Output file name without extension") String filename,
            @ToolParam(description = "List of slides, each with 'title' and 'bullets' (list of strings)") List<Slide> slides) {
        try {
            File outFile = new File(downloadDir, filename + ".pptx");
            outFile.getParentFile().mkdirs();

            try (XMLSlideShow ppt = new XMLSlideShow()) {
                // 使用宽屏尺寸（16:9）
                ppt.setPageSize(new java.awt.Dimension(960, 540));

                for (Slide slideData : slides) {
                    XSLFSlide slide = ppt.createSlide();

                    // 标题文本框
                    XSLFTextBox titleBox = slide.createTextBox();
                    titleBox.setAnchor(new Rectangle2D.Double(40, 20, 880, 70));
                    XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
                    XSLFTextRun titleRun = titlePara.addNewTextRun();
                    titleRun.setText(slideData.getTitle() != null ? slideData.getTitle() : "");
                    titleRun.setFontSize(28.0);
                    titleRun.setBold(true);

                    // 内容文本框（要点列表）
                    List<String> bullets = slideData.getBullets();
                    if (bullets != null && !bullets.isEmpty()) {
                        XSLFTextBox contentBox = slide.createTextBox();
                        contentBox.setAnchor(new Rectangle2D.Double(40, 110, 880, 380));
                        boolean first = true;
                        for (String bullet : bullets) {
                            XSLFTextParagraph para = first
                                    ? contentBox.getTextParagraphs().get(0)
                                    : contentBox.addNewTextParagraph();
                            first = false;
                            XSLFTextRun run = para.addNewTextRun();
                            run.setText("• " + bullet);
                            run.setFontSize(18.0);
                        }
                    }
                }

                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    ppt.write(fos);
                }
            }

            log.info("PPT 生成成功：{}", outFile.getAbsolutePath());
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            log.error("PPT 生成失败：{}", filename, e);
            return "PPT文件生成失败：" + e.getMessage();
        }
    }
}

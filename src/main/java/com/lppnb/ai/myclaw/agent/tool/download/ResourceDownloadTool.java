package com.lppnb.ai.myclaw.agent.tool.download;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;

/**
 * 资源下载工具：通过 URL 下载文件，保存至配置的下载目录，返回绝对路径。
 */
@Slf4j
@Component
public class ResourceDownloadTool {

    @Value("${tools.file.download-dir}")
    private String downloadDir;

    @PostConstruct
    public void init() {
        FileUtil.mkdir(downloadDir);
        log.info("资源下载目录就绪：{}", downloadDir);
    }

    @Tool(description = "Download a file from a URL and save it locally. Returns the absolute path of the saved file.")
    public String downloadResource(
            @ToolParam(description = "The download URL of the file") String url) {
        try {
            String filename = extractFilename(url);
            File destFile = new File(downloadDir, filename);
            HttpUtil.downloadFile(url, destFile);
            log.info("文件下载成功：{}", destFile.getAbsolutePath());
            return destFile.getAbsolutePath();
        } catch (Exception e) {
            log.error("文件下载失败：{}", url, e);
            return "下载失败：" + e.getMessage();
        }
    }

    private String extractFilename(String url) {
        try {
            String path = new URL(url).getPath();
            if (!path.isBlank()) {
                String name = path.substring(path.lastIndexOf('/') + 1);
                if (!name.isBlank()) {
                    return name;
                }
            }
        } catch (Exception e) {
            log.debug("无法从 URL 解析文件名：{}", url);
        }
        return "download_" + System.currentTimeMillis();
    }
}

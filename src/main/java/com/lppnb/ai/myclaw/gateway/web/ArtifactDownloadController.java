package com.lppnb.ai.myclaw.gateway.web;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Conditional;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 工具产物只读下载端点：仅暴露 {@code ${user.dir}/tmp/file} 目录内的文件，
 * 严格沙盒路径校验，防止目录穿越。
 */
@Slf4j
@RestController
@RequestMapping("/files")
@Conditional(WebChannelCondition.class)
public class ArtifactDownloadController {

    private static final Path DOWNLOAD_ROOT = Paths.get(System.getProperty("user.dir"), "tmp", "file")
            .toAbsolutePath().normalize();

    /** 下载产物文件：GET /api/files/** */
    @GetMapping("/**")
    public ResponseEntity<Resource> download(HttpServletRequest request) {
        String relative = extractRelativePath(request);
        if (relative == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Path target = DOWNLOAD_ROOT.resolve(relative).normalize();
        if (!target.startsWith(DOWNLOAD_ROOT)) {
            log.warn("Rejected file access outside sandbox: {}", target);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!Files.isRegularFile(target)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String filename = target.getFileName().toString();
        MediaType mediaType = MediaTypeFactory.getMediaType(filename).orElse(MediaType.APPLICATION_OCTET_STREAM);
        String contentDisposition = "attachment; filename*=UTF-8''"
                + encodeForHeader(filename);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(new FileSystemResource(target));
    }

    /** 从请求中提取 /files/ 之后的相对路径（已 URL 解码）；非法返回 null */
    private String extractRelativePath(HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (path == null) {
            path = request.getRequestURI();
        }
        String prefix = "/files/";
        int idx = path.indexOf(prefix);
        if (idx < 0) {
            return null;
        }
        String raw = path.substring(idx + prefix.length());
        if (raw.isEmpty()) {
            return null;
        }
        return URLDecoder.decode(raw, StandardCharsets.UTF_8);
    }

    /** RFC 5987 附件文件名编码（支持中文等非 ASCII 文件名） */
    private String encodeForHeader(String filename) {
        return java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    }
}

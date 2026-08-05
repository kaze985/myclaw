package com.lppnb.ai.myclaw.gateway.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 产物下载端点测试：正常下载、404、目录穿越 403。
 */
class ArtifactDownloadControllerTest {

    private static final File DOWNLOAD_ROOT =
            new File(System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "file");

    private static File sampleFile;

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ArtifactDownloadController()).build();

    @BeforeAll
    static void createSampleFile() throws Exception {
        DOWNLOAD_ROOT.mkdirs();
        sampleFile = new File(DOWNLOAD_ROOT, "sample-" + System.nanoTime() + ".txt");
        java.nio.file.Files.writeString(sampleFile.toPath(), "hello artifact", StandardCharsets.UTF_8);
    }

    @AfterAll
    static void cleanup() {
        if (sampleFile != null) {
            sampleFile.delete();
        }
    }

    @Test
    void downloadExistingFileSucceeds() throws Exception {
        mockMvc.perform(get("/files/" + sampleFile.getName()))
                .andExpect(status().isOk())
                .andExpect(content().string("hello artifact"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment; filename*=UTF-8''")));
    }

    @Test
    void downloadMissingFileReturns404() throws Exception {
        mockMvc.perform(get("/files/no-such-file-" + System.nanoTime() + ".pdf"))
                .andExpect(status().isNotFound());
    }

    @Test
    void traversalOutsideSandboxReturns403() throws Exception {
        mockMvc.perform(get("/files/..%2F..%2Fpom.xml"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/files/..%5C..%5Cpom.xml"))
                .andExpect(status().isForbidden());
    }

    @Test
    void directoryIsNotDownloadable() throws Exception {
        mockMvc.perform(get("/files/"))
                .andExpect(status().isBadRequest());
    }
}

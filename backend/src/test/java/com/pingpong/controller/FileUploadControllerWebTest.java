package com.pingpong.controller;

import com.pingpong.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FileUploadController 的控制器层测试。
 *
 * 图片校验与落盘逻辑位于 ImageStorageService，因此这里将存储服务替换为 Mock，
 * 只校验控制器的职责：请求编排、结果包装与文件下载（含路径净化与 404 分支）。
 * 真实存储链路由 UploadEndpointIntegrationTest 覆盖。
 */
class FileUploadControllerWebTest {

    @TempDir
    Path tempDir;

    private ImageStorageService imageStorageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        imageStorageService = mock(ImageStorageService.class);
        FileUploadController controller = new FileUploadController();
        ReflectionTestUtils.setField(controller, "imageStorageService", imageStorageService);
        ReflectionTestUtils.setField(controller, "uploadPath", tempDir.toFile().getAbsolutePath());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void uploadEquipmentImageShouldDelegateToStorageServiceWithEquipmentFolder() throws Exception {
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("url", "/api/upload/equipment/racket.png");
        stored.put("contentType", "image/png");
        when(imageStorageService.storeImage(any(), eq("equipment"))).thenReturn(stored);

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "racket.png",
            "image/png",
            "fake-png-bytes".getBytes()
        );

        mockMvc.perform(multipart("/upload/equipment-image").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.url").value("/api/upload/equipment/racket.png"));

        verify(imageStorageService).storeImage(any(), eq("equipment"));
    }

    @Test
    void uploadFileShouldDelegateToStorageServiceWithoutFolder() throws Exception {
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("url", "/api/upload/generic.png");
        when(imageStorageService.storeImage(any(), isNull())).thenReturn(stored);

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "generic.png",
            "image/png",
            "fake-png-bytes".getBytes()
        );

        mockMvc.perform(multipart("/upload").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.url").value("/api/upload/generic.png"));

        verify(imageStorageService).storeImage(any(), isNull());
    }

    @Test
    void downloadFileShouldServeExistingFileFromUploadPath() throws Exception {
        byte[] payload = "cached-image".getBytes();
        Files.write(tempDir.resolve("cached.png"), payload);

        mockMvc.perform(get("/upload/cached.png"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "public, max-age=86400"))
            .andExpect(content().bytes(payload));
    }

    @Test
    void downloadNestedFileShouldServeFileInsideSanitizedFolder() throws Exception {
        byte[] payload = "nested-image".getBytes();
        Path folder = Files.createDirectory(tempDir.resolve("equipment"));
        Files.write(folder.resolve("nested.png"), payload);

        mockMvc.perform(get("/upload/equipment/nested.png"))
            .andExpect(status().isOk())
            .andExpect(content().bytes(payload));
    }

    @Test
    void downloadFileShouldReturnNotFoundWhenFileMissing() throws Exception {
        mockMvc.perform(get("/upload/not-exists.png"))
            .andExpect(status().isNotFound());
    }
}

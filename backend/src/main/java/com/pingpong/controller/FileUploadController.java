package com.pingpong.controller;

import cn.hutool.core.io.FileUtil;
import com.pingpong.common.Result;
import com.pingpong.service.ImageStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@RestController
@RequestMapping("/upload")
@CrossOrigin
public class FileUploadController {

    @Autowired
    private ImageStorageService imageStorageService;

    @Value("${upload.path}")
    private String uploadPath;

    @PostMapping
    public Result<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.success(imageStorageService.storeImage(file, null));
    }

    @PostMapping("/equipment-image")
    public Result<Map<String, Object>> uploadEquipmentImage(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.success(imageStorageService.storeImage(file, "equipment"));
    }

    @GetMapping("/{filename:.+}")
    public void downloadFile(@PathVariable String filename, HttpServletResponse response) throws IOException {
        writeFileToResponse(null, filename, response);
    }

    @GetMapping("/{folder}/{filename:.+}")
    public void downloadNestedFile(
        @PathVariable String folder,
        @PathVariable String filename,
        HttpServletResponse response
    ) throws IOException {
        writeFileToResponse(folder, filename, response);
    }

    private void writeFileToResponse(String folder, String filename, HttpServletResponse response) throws IOException {
        String safeFolder = sanitizeFolder(folder);
        String safeFilename = sanitizeFilename(filename);
        File file = StringUtils.hasText(safeFolder)
            ? new File(new File(uploadPath, safeFolder), safeFilename)
            : new File(uploadPath, safeFilename);

        if (!file.exists() || !file.isFile()) {
            response.setStatus(404);
            return;
        }

        String contentType = Files.probeContentType(file.toPath());
        if (!StringUtils.hasText(contentType)) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "public, max-age=86400");
        FileUtil.writeToStream(file, response.getOutputStream());
    }

    private String sanitizeFolder(String folder) {
        if (!StringUtils.hasText(folder)) {
            return null;
        }
        return folder.replaceAll("[^a-zA-Z0-9_-]", "");
    }

    private String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        return filename.replaceAll("[\\\\/]", "");
    }
}

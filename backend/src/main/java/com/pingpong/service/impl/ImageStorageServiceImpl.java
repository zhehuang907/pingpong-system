package com.pingpong.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.pingpong.exception.BusinessException;
import com.pingpong.service.ImageStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ImageStorageServiceImpl implements ImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "webp"));
    private static final Set<String> ALLOWED_CONTENT_TYPES = new HashSet<>(Arrays.asList(
        MediaType.IMAGE_JPEG_VALUE,
        MediaType.IMAGE_PNG_VALUE,
        MediaType.IMAGE_GIF_VALUE,
        "image/webp"
    ));

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${upload.max-size:5242880}")
    private Long maxSize;

    @Override
    public Map<String, Object> storeImage(MultipartFile file, String folder) throws IOException {
        validateImage(file);

        String originalFilename = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "image";
        String extension = FileUtil.getSuffix(originalFilename).toLowerCase(Locale.ROOT);
        String safeFolder = sanitizeFolder(folder);
        String filename = IdUtil.fastSimpleUUID() + "." + extension;

        File baseDir = StringUtils.hasText(safeFolder) ? new File(uploadPath, safeFolder) : new File(uploadPath);
        FileUtil.mkdir(baseDir);

        File dest = new File(baseDir, filename);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("filename", StringUtils.hasText(safeFolder) ? safeFolder + "/" + filename : filename);
        data.put("url", StringUtils.hasText(safeFolder) ? "/api/upload/" + safeFolder + "/" + filename : "/api/upload/" + filename);
        data.put("originalName", originalFilename);
        data.put("size", file.getSize());
        data.put("contentType", file.getContentType());
        return data;
    }

    private void validateImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        if (file.getSize() > defaultMaxSize()) {
            throw new BusinessException("图片大小不能超过 5MB");
        }

        String originalFilename = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "image";
        String extension = FileUtil.getSuffix(originalFilename).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 JPG、JPEG、PNG、GIF、WEBP 图片格式");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("文件类型不合法，请上传标准图片文件");
        }

        if ("webp".equals(extension) || "image/webp".equalsIgnoreCase(contentType)) {
            if (!isWebp(file)) {
                throw new BusinessException("WEBP 文件内容校验失败");
            }
            return;
        }

        BufferedImage image;
        try (InputStream inputStream = file.getInputStream()) {
            image = ImageIO.read(inputStream);
        }
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw new BusinessException("文件内容不是有效图片");
        }
    }

    private boolean isWebp(MultipartFile file) throws IOException {
        byte[] header = new byte[12];
        try (InputStream inputStream = file.getInputStream()) {
            if (inputStream.read(header) < 12) {
                return false;
            }
        }
        String riff = new String(header, 0, 4);
        String webp = new String(header, 8, 4);
        return "RIFF".equals(riff) && "WEBP".equals(webp);
    }

    private long defaultMaxSize() {
        return maxSize == null || maxSize <= 0 ? 5 * 1024 * 1024L : maxSize;
    }

    private String sanitizeFolder(String folder) {
        if (!StringUtils.hasText(folder)) {
            return null;
        }
        return folder.replaceAll("[^a-zA-Z0-9_-]", "");
    }
}

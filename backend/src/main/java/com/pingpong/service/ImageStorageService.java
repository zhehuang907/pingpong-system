package com.pingpong.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface ImageStorageService {

    Map<String, Object> storeImage(MultipartFile file, String folder) throws IOException;
}

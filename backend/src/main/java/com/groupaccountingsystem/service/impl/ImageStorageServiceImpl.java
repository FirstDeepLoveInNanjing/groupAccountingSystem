package com.groupaccountingsystem.service.impl;

import com.groupaccountingsystem.service.ImageStorageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageStorageServiceImpl implements ImageStorageService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    @Override
    public String storeImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("\u8bf7\u4e0a\u4f20\u56fe\u7247\u6587\u4ef6");
        }
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = getExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("\u53ea\u652f\u6301 jpg\u3001jpeg\u3001png \u683c\u5f0f\u56fe\u7247");
        }

        String safeFolder = folder == null ? "common" : folder.replaceAll("[^a-zA-Z0-9_-]", "");
        String filename = UUID.randomUUID() + "." + extension;
        Path targetDir = Paths.get("uploads", safeFolder).toAbsolutePath().normalize();
        Path targetFile = targetDir.resolve(filename).normalize();
        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile);
        } catch (IOException e) {
            throw new RuntimeException("\u4fdd\u5b58\u56fe\u7247\u5931\u8d25", e);
        }
        return "/uploads/" + safeFolder + "/" + filename;
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}

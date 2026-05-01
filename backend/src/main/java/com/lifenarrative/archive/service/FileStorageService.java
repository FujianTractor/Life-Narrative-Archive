package com.lifenarrative.archive.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.storage.upload-dir}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public StoredFile storeArchiveImage(String archiveId, MultipartFile file) throws IOException {
        return storeArchiveAsset("images", archiveId, file, "image-upload");
    }

    public StoredFile storeArchiveVideo(String archiveId, MultipartFile file) throws IOException {
        return storeArchiveAsset("videos", archiveId, file, "video-upload");
    }

    private StoredFile storeArchiveAsset(String bucket, String archiveId, MultipartFile file, String fallbackName)
            throws IOException {
        String originalFilename = safeFileName(file.getOriginalFilename(), fallbackName);
        String extension = getExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);

        Path targetDirectory = uploadRoot.resolve(bucket).resolve(archiveId).normalize();
        Files.createDirectories(targetDirectory);

        Path targetFile = targetDirectory.resolve(storedFilename).normalize();
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }

        String urlPath = "/uploads/" + bucket + "/" + archiveId + "/" + storedFilename;
        String contentType = file.getContentType() == null || file.getContentType().isBlank()
                ? "application/octet-stream"
                : file.getContentType();

        return new StoredFile(
                originalFilename,
                targetFile.toString(),
                urlPath,
                contentType,
                file.getSize()
        );
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }

    private String safeFileName(String originalFilename, String fallback) {
        String candidate = originalFilename == null || originalFilename.isBlank() ? fallback : originalFilename;
        String normalized = Normalizer.normalize(candidate, Normalizer.Form.NFKC)
                .replace("\\", "_")
                .replace("/", "_")
                .replaceAll("[\\r\\n]", "_")
                .trim();
        return normalized.isBlank() ? fallback : normalized;
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredFile(
            String originalFilename,
            String filePath,
            String urlPath,
            String mimeType,
            long sizeBytes
    ) {
    }
}

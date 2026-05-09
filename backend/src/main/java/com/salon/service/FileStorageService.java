package com.salon.service;

import com.salon.exception.InvalidOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private static final Set<String> ALLOWED_PHOTO_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/webm"
    );
    private static final long MAX_PHOTO_SIZE = 5 * 1024 * 1024L;  // 5 MB
    private static final long MAX_VIDEO_SIZE = 30 * 1024 * 1024L; // 30 MB

    public String storePhoto(MultipartFile file, String subfolder) {
        validatePhoto(file);
        return store(file, subfolder);
    }

    public String storeVideo(MultipartFile file, String subfolder) {
        validateVideo(file);
        return store(file, subfolder);
    }

    /**
     * Decode a base64 data-URL (e.g. "data:image/jpeg;base64,/9j/4AAQ...")
     * and save it as a file in the given subfolder.
     * Returns the server path like /api/v1/files/portfolio/uuid.jpg
     */
    public String storeBase64(String dataUrl, String subfolder) {
        if (dataUrl == null || !dataUrl.startsWith("data:")) {
            throw new InvalidOperationException("Invalid data URL");
        }
        // Format: data:<mimeType>;base64,<data>
        String[] parts = dataUrl.split(",", 2);
        if (parts.length != 2) throw new InvalidOperationException("Malformed data URL");

        String meta = parts[0]; // e.g. "data:image/jpeg;base64"
        String base64Data = parts[1];

        String mimeType = meta.substring(5, meta.indexOf(';')); // e.g. "image/jpeg"
        String ext = mimeType.contains("/") ? mimeType.split("/")[1] : "bin";
        // Normalize common extensions
        if ("jpeg".equals(ext)) ext = "jpg";

        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(base64Data);
            String filename = UUID.randomUUID() + "." + ext;
            Path dir = Paths.get(System.getProperty("user.dir"), uploadDir, subfolder);
            Files.createDirectories(dir);
            Path dest = dir.resolve(filename);
            Files.write(dest, bytes);
            // Return path served by FileController
            return "/api/v1/files/" + subfolder + "/" + filename;
        } catch (IOException e) {
            throw new InvalidOperationException("Failed to store base64 file: " + e.getMessage());
        }
    }

    public void deleteFile(String filePath) {        if (filePath == null) return;
        try {
            Path path = Paths.get(filePath.startsWith("/") ? filePath.substring(1) : filePath);
            Files.deleteIfExists(path);
            log.info("Deleted file: {}", filePath);
        } catch (IOException e) {
            log.warn("Could not delete file: {}", filePath);
        }
    }

    private String store(MultipartFile file, String subfolder) {
        try {
            String ext = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + "." + ext;
            // Resolve relative to the current working directory (project root when running via mvn)
            Path dir = Paths.get(System.getProperty("user.dir"), uploadDir, subfolder);
            Files.createDirectories(dir);
            Path dest = dir.resolve(filename);
            file.transferTo(dest.toFile());
            return "/" + uploadDir + "/" + subfolder + "/" + filename;
        } catch (IOException e) {
            throw new InvalidOperationException("Failed to store file: " + e.getMessage());
        }
    }

    private void validatePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new InvalidOperationException("File is empty");
        if (!ALLOWED_PHOTO_TYPES.contains(file.getContentType()))
            throw new InvalidOperationException("Invalid photo type. Allowed: JPG, PNG, WEBP");
        if (file.getSize() > MAX_PHOTO_SIZE)
            throw new InvalidOperationException("Photo exceeds 5 MB limit");
    }

    private void validateVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new InvalidOperationException("File is empty");
        if (!ALLOWED_VIDEO_TYPES.contains(file.getContentType()))
            throw new InvalidOperationException("Invalid video type. Allowed: MP4, WEBM");
        if (file.getSize() > MAX_VIDEO_SIZE)
            throw new InvalidOperationException("Video exceeds 30 MB limit");
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}

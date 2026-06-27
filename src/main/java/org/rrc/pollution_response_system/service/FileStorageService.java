package org.rrc.pollution_response_system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads/profiles}")
    private String uploadDir;

    @Value("${app.upload.max-size:5242880}") // 5MB default
    private long maxFileSize;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    /**
     * Store profile image with validation and security checks
     * 
     * @param file The uploaded file
     * @param userId User ID for naming convention
     * @return Relative path to stored file
     * @throws IOException If storage fails
     * @throws IllegalArgumentException If validation fails
     */
    public String storeProfileImage(MultipartFile file, Long userId) throws IOException {
        // Validation: File must not be empty
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file");
        }

        // Validation: File size
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + "MB");
        }

        // Validation: Content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: JPG, PNG, GIF, WEBP");
        }

        // Validation: File extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Invalid file extension. Allowed: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        // Generate secure filename: user_{userId}_{uuid}.{extension}
        String secureFilename = String.format("user_%d_%s.%s", 
                userId, 
                UUID.randomUUID().toString(), 
                extension);

        // Ensure upload directory exists
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Store file
        Path targetLocation = uploadPath.resolve(secureFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Return relative path for storage in database
        return "/" + uploadDir + "/" + secureFilename;
    }

    /**
     * Delete profile image file
     * 
     * @param imagePath Relative path to image
     * @return true if deleted successfully
     */
    public boolean deleteProfileImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return false;
        }

        try {
            // Remove leading slash if present
            String relativePath = imagePath.startsWith("/") ? imagePath.substring(1) : imagePath;
            Path filePath = Paths.get(relativePath).toAbsolutePath().normalize();
            
            // Security check: ensure file is within upload directory
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!filePath.startsWith(uploadPath)) {
                throw new SecurityException("Invalid file path");
            }

            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return false;
        }

        try {
            String relativePath = imagePath.startsWith("/") ? imagePath.substring(1) : imagePath;
            Path filePath = Paths.get(relativePath).toAbsolutePath().normalize();
            return Files.exists(filePath);
        } catch (Exception e) {
            return false;
        }
    }
}

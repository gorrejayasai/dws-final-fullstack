package com.cognizant.digitalwalletsystem.service;

import com.cognizant.digitalwalletsystem.exception.InvalidDocumentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

/**
 * Handles persistence of KYC documents on the local filesystem.
 *
 * Files are stored under <code>${app.upload.dir}/user_{userId}/{uuid}_{originalName}</code>.
 * Only the RELATIVE path is returned to be stored in DB ({@link com.cognizant.digitalwalletsystem.entity.KycDocument#fileReference}).
 */
@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir:./uploads/kyc}")
    private String uploadDir;

    /**
     * Save the uploaded file under a per-user folder.
     *
     * @param file   the multipart file uploaded by the user
     * @param userId the owning userId — used to namespace storage
     * @return relative path that should be persisted in the DB (e.g. {@code user_42/uuid_aadhaar.jpg})
     */
    public String saveFile(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentException("Document file is required and cannot be empty.");
        }

        // Defensive: clean the original filename to block directory traversal
        String originalFileName = StringUtils.cleanPath(
                Objects.requireNonNullElse(file.getOriginalFilename(), "document"));

        // UUID prefix prevents collisions and keeps user-supplied names out of the filesystem keyspace
        String uniqueFileName = UUID.randomUUID() + "_" + originalFileName;

        Path userFolderPath = Paths.get(uploadDir).resolve("user_" + userId);

        try {
            if (!Files.exists(userFolderPath)) {
                Files.createDirectories(userFolderPath);
            }

            Path targetLocation = userFolderPath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = "user_" + userId + "/" + uniqueFileName;
            log.info("Stored KYC document for user {} at {}", userId, relativePath);
            return relativePath;
        } catch (IOException e) {
            log.error("Failed to store KYC document for user {}: {}", userId, e.getMessage(), e);
            throw new InvalidDocumentException("Failed to store document: " + e.getMessage());
        }
    }

    /**
     * Load a stored document as a Spring Resource so the controller can stream it back to the client.
     *
     * @param relativePath the relative path previously returned by {@link #saveFile}
     * @return Resource pointing to the file on disk
     */
    public Resource loadFile(String relativePath) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(relativePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new InvalidDocumentException("Document file not found or unreadable: " + relativePath);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new InvalidDocumentException("Invalid document path: " + relativePath);
        }
    }

    /**
     * Best-effort delete of a previously stored document. Errors are logged but never thrown,
     * so deletion failures cannot block the parent KYC operation.
     */
    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Path filePath = Paths.get(uploadDir).resolve(relativePath).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete KYC document at {}: {}", relativePath, e.getMessage());
        }
    }
}
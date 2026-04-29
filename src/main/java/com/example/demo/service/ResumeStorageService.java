package com.example.demo.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeStorageService {

    private static final long MAX_RESUME_SIZE_BYTES = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");

    private final Path rootLocation;

    public ResumeStorageService(@Value("${app.resume.storage-root:uploads/resumes}") String storageRoot) {
        this.rootLocation = Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    public String storeResume(MultipartFile file, Long facilityId, Long applicantId) {
        validateResume(file);
        String extension = resolveExtension(file);
        String fileName = "resume." + extension;

        Path folder = rootLocation.resolve(String.valueOf(facilityId)).resolve(String.valueOf(applicantId)).normalize();
        Path destinationFile = folder.resolve(fileName).normalize();

        if (!destinationFile.startsWith(rootLocation)) {
            throw new IllegalArgumentException("Invalid storage path");
        }

        try {
            Files.createDirectories(folder);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store resume", e);
        }

        return String.format("%d/%d/%s", facilityId, applicantId, fileName);
    }

    public Resource loadResumeAsResource(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            throw new IllegalArgumentException("Resume path is empty");
        }
        Path filePath = rootLocation.resolve(relativePath).normalize();
        if (!filePath.startsWith(rootLocation)) {
            throw new IllegalArgumentException("Invalid resume path");
        }
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("Resume file not found");
            }
            return resource;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load resume", e);
        }
    }

    private void validateResume(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file is required");
        }

        if (file.getSize() > MAX_RESUME_SIZE_BYTES) {
            throw new IllegalArgumentException("Resume must be 5MB or smaller");
        }

        String contentType = file.getContentType();
        String extension = resolveExtension(file);
        boolean validContentType = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT));

        if (!validContentType && !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Resume must be PDF, DOC, or DOCX");
        }
    }

    private String resolveExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (ALLOWED_EXTENSIONS.contains(ext)) {
                return ext;
            }
        }

        String contentType = file.getContentType();
        if ("application/pdf".equalsIgnoreCase(contentType)) {
            return "pdf";
        }
        if ("application/msword".equalsIgnoreCase(contentType)) {
            return "doc";
        }
        if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(contentType)) {
            return "docx";
        }

        throw new IllegalArgumentException("Unable to determine resume file extension");
    }
}

package com.cafe.domain.menu.support;

import com.cafe.common.error.MenuErrorCode;
import com.cafe.common.error.MenuException;
import com.cafe.infrastructure.storage.MediaStorageProperties;
import com.cafe.infrastructure.storage.ObjectStorageClient;
import com.cafe.infrastructure.storage.UploadedObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MenuImageService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final ObjectStorageClient objectStorageClient;
    private final MediaStorageProperties properties;

    public UploadedObject uploadOptional(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        validateImage(file);
        return objectStorageClient.upload(file, createStorageKey(file));
    }

    public UploadedObject uploadRequired(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MenuException(MenuErrorCode.INVALID_MENU_IMAGE);
        }

        validateImage(file);
        return objectStorageClient.upload(file, createStorageKey(file));
    }

    public void deleteQuietly(String key) {
        try {
            objectStorageClient.delete(key);
        } catch (RuntimeException exception) {
            log.warn("Failed to delete uploaded menu image. key={}, message={}", key, exception.getMessage());
        }
    }

    private void validateImage(MultipartFile file) {
        if (file.getSize() > properties.getMaxImageSizeBytes()) {
            throw new MenuException(MenuErrorCode.MENU_IMAGE_TOO_LARGE);
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new MenuException(MenuErrorCode.INVALID_MENU_IMAGE);
        }

        String extension = extractExtension(file);
        Set<String> allowedExtensions = properties.normalizedAllowedImageExtensions();
        if (!allowedExtensions.contains(extension)) {
            throw new MenuException(MenuErrorCode.INVALID_MENU_IMAGE);
        }
    }

    private String createStorageKey(MultipartFile file) {
        String date = LocalDate.now().format(DATE_FORMATTER);
        return properties.normalizedKeyPrefix()
                + "/"
                + date
                + "/"
                + UUID.randomUUID()
                + "."
                + extractExtension(file);
    }

    private String extractExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename)) {
            throw new MenuException(MenuErrorCode.INVALID_MENU_IMAGE);
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            throw new MenuException(MenuErrorCode.INVALID_MENU_IMAGE);
        }

        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}

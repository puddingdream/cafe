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
    // 메뉴 이미지 검증, 저장 key 생성, 외부 저장소 업로드/삭제를 담당한다.
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final ObjectStorageClient objectStorageClient;
    private final MediaStorageProperties properties;

    public UploadedObject uploadOptional(MultipartFile file) {
        // 수정 API에서는 이미지가 없으면 기존 이미지를 유지한다.
        if (file == null || file.isEmpty()) {
            return null;
        }

        validateImage(file);
        return objectStorageClient.upload(file, createStorageKey(file));
    }

    public UploadedObject uploadRequired(MultipartFile file) {
        // 생성 API에서는 대표 이미지가 필수다.
        if (file == null || file.isEmpty()) {
            throw new MenuException(MenuErrorCode.INVALID_MENU_IMAGE);
        }

        validateImage(file);
        return objectStorageClient.upload(file, createStorageKey(file));
    }

    public void deleteQuietly(String key) {
        // 보상 삭제/기존 이미지 삭제 실패가 본 트랜잭션 성공을 뒤집지 않도록 로그만 남긴다.
        if (!StringUtils.hasText(key)) {
            return;
        }

        try {
            objectStorageClient.delete(key);
        } catch (RuntimeException exception) {
            log.warn("Failed to delete uploaded menu image. key={}, message={}", key, exception.getMessage());
        }
    }

    private void validateImage(MultipartFile file) {
        // content-type과 확장자를 모두 확인해 이미지가 아닌 파일 업로드를 막는다.
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
        // 날짜 디렉터리와 UUID를 사용해 파일명 충돌을 피한다.
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
        // 확장자 검증은 저장 key 생성과 허용 확장자 체크에 함께 사용된다.
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

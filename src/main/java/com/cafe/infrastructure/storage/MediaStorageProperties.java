package com.cafe.infrastructure.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@ConfigurationProperties(prefix = "media.storage")
public class MediaStorageProperties {
    // application.yml/.env의 media.storage.* 값을 타입 안전하게 받는다.
    private boolean enabled;
    private String bucket;
    private String region = "ap-northeast-2";
    private String endpoint;
    private String publicBaseUrl;
    private String accessKey;
    private String secretKey;
    private boolean pathStyleAccess;
    private String keyPrefix = "menu-images";
    private long maxImageSizeBytes = 10 * 1024 * 1024;
    private Set<String> allowedImageExtensions = new LinkedHashSet<>(List.of("jpg", "jpeg", "png", "webp"));

    public boolean hasStaticCredentials() {
        // access-key/secret-key가 둘 다 있으면 명시 credential을 사용한다.
        return StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey);
    }

    public String normalizedKeyPrefix() {
        // 저장 key prefix 앞뒤 슬래시를 정리해 중복 구분자를 방지한다.
        if (!StringUtils.hasText(keyPrefix)) {
            return "menu-images";
        }
        return keyPrefix.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    public Set<String> normalizedAllowedImageExtensions() {
        // 쉼표로 들어온 확장자 문자열도 Set 형태로 정규화한다.
        return allowedImageExtensions.stream()
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }
}

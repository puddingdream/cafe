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
        return StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey);
    }

    public String normalizedKeyPrefix() {
        if (!StringUtils.hasText(keyPrefix)) {
            return "menu-images";
        }
        return keyPrefix.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    public Set<String> normalizedAllowedImageExtensions() {
        return allowedImageExtensions.stream()
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }
}

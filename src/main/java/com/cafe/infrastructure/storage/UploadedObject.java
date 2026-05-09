package com.cafe.infrastructure.storage;

public record UploadedObject(
        String key,
        String url,
        String contentType,
        long size
) {
}

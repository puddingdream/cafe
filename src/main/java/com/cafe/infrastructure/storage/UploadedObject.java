package com.cafe.infrastructure.storage;

public record UploadedObject(
        // 외부 저장소에 업로드된 객체의 key와 접근 정보를 담는다.
        String key,
        String url,
        String contentType,
        long size
) {
}

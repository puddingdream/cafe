package com.cafe.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ObjectStorageClient {
    // 메뉴 이미지 같은 객체 파일 저장소 구현체가 지켜야 하는 최소 계약이다.
    UploadedObject upload(MultipartFile file, String key);

    void delete(String key);
}

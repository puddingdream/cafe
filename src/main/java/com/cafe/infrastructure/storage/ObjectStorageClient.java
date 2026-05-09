package com.cafe.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ObjectStorageClient {
    UploadedObject upload(MultipartFile file, String key);

    void delete(String key);
}

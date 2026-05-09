package com.cafe.infrastructure.storage;

import com.cafe.common.error.MenuErrorCode;
import com.cafe.common.error.MenuException;
import org.springframework.web.multipart.MultipartFile;

public class DisabledObjectStorageClient implements ObjectStorageClient {
    @Override
    public UploadedObject upload(MultipartFile file, String key) {
        throw new MenuException(MenuErrorCode.MENU_IMAGE_STORAGE_DISABLED);
    }

    @Override
    public void delete(String key) {
        throw new MenuException(MenuErrorCode.MENU_IMAGE_STORAGE_DISABLED);
    }
}

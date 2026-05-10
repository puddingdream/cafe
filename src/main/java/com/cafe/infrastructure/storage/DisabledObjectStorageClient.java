package com.cafe.infrastructure.storage;

import com.cafe.common.error.MenuErrorCode;
import com.cafe.common.error.MenuException;
import org.springframework.web.multipart.MultipartFile;

public class DisabledObjectStorageClient implements ObjectStorageClient {
    @Override
    public UploadedObject upload(MultipartFile file, String key) {
        // storage 설정 없이 업로드를 시도하면 조용히 무시하지 않고 명확한 예외를 던진다.
        throw new MenuException(MenuErrorCode.MENU_IMAGE_STORAGE_DISABLED);
    }

    @Override
    public void delete(String key) {
        // 삭제도 동일하게 "저장소가 설정되지 않음"을 알려준다.
        throw new MenuException(MenuErrorCode.MENU_IMAGE_STORAGE_DISABLED);
    }
}

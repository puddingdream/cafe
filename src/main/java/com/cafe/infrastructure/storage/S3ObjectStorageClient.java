package com.cafe.infrastructure.storage;

import com.cafe.common.error.MenuErrorCode;
import com.cafe.common.error.MenuException;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;

public class S3ObjectStorageClient implements ObjectStorageClient {
    // AWS SDK S3Client를 이용해 메뉴 이미지를 업로드/삭제하는 실제 구현체다.
    private final S3Client s3Client;
    private final MediaStorageProperties properties;

    public S3ObjectStorageClient(S3Client s3Client, MediaStorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @Override
    public UploadedObject upload(MultipartFile file, String key) {
        // MultipartFile stream을 그대로 S3에 업로드하고 공개 URL 정보를 반환한다.
        if (!StringUtils.hasText(properties.getBucket())) {
            throw new MenuException(MenuErrorCode.MENU_IMAGE_STORAGE_DISABLED);
        }

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest.Builder request = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .contentLength(file.getSize());

            if (StringUtils.hasText(file.getContentType())) {
                // contentType은 브라우저/스토리지에서 이미지 미리보기와 다운로드 처리에 사용된다.
                request.contentType(file.getContentType());
            }

            s3Client.putObject(request.build(), RequestBody.fromInputStream(inputStream, file.getSize()));
            return new UploadedObject(key, buildPublicUrl(key), file.getContentType(), file.getSize());
        } catch (IOException | SdkException exception) {
            throw new MenuException(MenuErrorCode.MENU_IMAGE_UPLOAD_FAILED, exception);
        }
    }

    @Override
    public void delete(String key) {
        // key가 없거나 bucket 설정이 없으면 삭제할 대상이 없으므로 조용히 종료한다.
        if (!StringUtils.hasText(key) || !StringUtils.hasText(properties.getBucket())) {
            return;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build());
        } catch (SdkException exception) {
            throw new MenuException(MenuErrorCode.MENU_IMAGE_DELETE_FAILED, exception);
        }
    }

    private String buildPublicUrl(String key) {
        // CDN/base URL이 있으면 우선 사용하고, 없으면 AWS S3 기본 공개 URL 형식을 만든다.
        if (StringUtils.hasText(properties.getPublicBaseUrl())) {
            return properties.getPublicBaseUrl().replaceAll("/+$", "") + "/" + key;
        }
        return "https://" + properties.getBucket() + ".s3." + properties.getRegion() + ".amazonaws.com/" + key;
    }
}

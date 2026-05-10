package com.cafe.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MenuErrorCode implements ErrorCodeType {
    // 메뉴 관리와 메뉴 이미지 업로드에서 사용하는 에러 코드다.
    FORBIDDEN_MENU_MANAGEMENT(HttpStatus.FORBIDDEN, "MENU001", "관리자만 가능한 기능입니다."),
    INVALID_MENU_PRICE(HttpStatus.BAD_REQUEST, "MENU002", "메뉴 가격은 0원보다 커야 합니다."),
    INVALID_MENU_CATEGORY(HttpStatus.BAD_REQUEST, "MENU003", "존재하지 않는 메뉴 카테고리입니다."),
    INVALID_MENU_IMAGE(HttpStatus.BAD_REQUEST, "MENU004", "지원하지 않는 메뉴 이미지입니다."),
    MENU_IMAGE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "MENU005", "메뉴 이미지 용량이 너무 큽니다."),
    MENU_IMAGE_STORAGE_DISABLED(HttpStatus.SERVICE_UNAVAILABLE, "MENU006", "메뉴 이미지 저장소가 설정되어 있지 않습니다."),
    MENU_IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MENU007", "메뉴 이미지 업로드에 실패했습니다."),
    MENU_IMAGE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MENU008", "메뉴 이미지 삭제에 실패했습니다."),
    INVALID_MENU_STATUS(HttpStatus.BAD_REQUEST, "MENU009", "존재하지 않는 메뉴 상태입니다."),
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "MENU010", "메뉴를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

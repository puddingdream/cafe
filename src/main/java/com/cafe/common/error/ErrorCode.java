package com.cafe.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
// HTTP 상태, 서비스 코드, 사용자 메시지를 한 곳에서 관리한다.
public enum ErrorCode implements ErrorCodeType {

    // 1. Common (공통)
    INVALID_AUTHENTICATION(HttpStatus.UNAUTHORIZED, "C001", "인증이 필요합니다. (로그인 만료 포함)"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C002", "해당 기능에 대한 접근 권한이 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "C003", "잘못된 요청 형식 또는 파라미터입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C005", "서버 내부 오류가 발생했습니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "C006", "도배 방지를 위해 요청 횟수가 제한되었습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C007", "잘못된 입력값입니다."),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "C008", "업로드 가능한 최대 용량을 초과했습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}

package com.cafe.common.error;

public class AiException extends BusinessException {
    // AI 외부 연동 실패를 도메인 예외로 감싸 공통 응답 형식을 유지한다.
    public AiException(AiErrorCode errorCode) {
        super(errorCode);
    }

    public AiException(AiErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

package com.cafe.common.error;

import lombok.Getter;

@Getter
public class PointException extends BusinessException {
    // 포인트 도메인의 비즈니스 예외다.

    public PointException(ErrorCodeType errorCode) {
        super(errorCode);
    }

    public PointException(ErrorCodeType errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

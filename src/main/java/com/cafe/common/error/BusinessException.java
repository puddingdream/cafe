package com.cafe.common.error;

import lombok.Getter;

/**
 * 모든 비즈니스 로직 예외의 최상위 클래스
 */
@Getter
public abstract class BusinessException extends RuntimeException {

    private final ErrorCodeType errorCode;

    public BusinessException(ErrorCodeType errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCodeType errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}

package com.cafe.common.error;

import lombok.Getter;

@Getter
public class MenuException extends BusinessException {
    // 메뉴 도메인의 비즈니스 예외다.

    public MenuException(ErrorCodeType errorCode) {
        super(errorCode);
    }

    public MenuException(ErrorCodeType errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

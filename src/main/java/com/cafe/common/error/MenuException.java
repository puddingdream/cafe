package com.cafe.common.error;

import lombok.Getter;

@Getter
public class MenuException extends BusinessException {

    public MenuException(ErrorCodeType errorCode) {
        super(errorCode);
    }

    public MenuException(ErrorCodeType errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

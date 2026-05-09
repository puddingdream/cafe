package com.cafe.common.error;

import lombok.Getter;

@Getter
public class PointException extends BusinessException {

    public PointException(ErrorCodeType errorCode) {
        super(errorCode);
    }

    public PointException(ErrorCodeType errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

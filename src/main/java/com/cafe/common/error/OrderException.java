package com.cafe.common.error;

import lombok.Getter;

@Getter
public class OrderException extends BusinessException {

    public OrderException(ErrorCodeType errorCode) {
        super(errorCode);
    }

    public OrderException(ErrorCodeType errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

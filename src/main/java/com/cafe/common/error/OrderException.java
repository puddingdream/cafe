package com.cafe.common.error;

import lombok.Getter;

@Getter
public class OrderException extends BusinessException {
    // 주문 도메인의 비즈니스 예외다.

    public OrderException(ErrorCodeType errorCode) {
        super(errorCode);
    }

    public OrderException(ErrorCodeType errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

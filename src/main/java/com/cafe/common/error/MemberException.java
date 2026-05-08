package com.cafe.common.error;

import lombok.Getter;

@Getter
public class MemberException extends BusinessException {

    public MemberException(ErrorCodeType errorCode) {
        super(errorCode);
    }

    public MemberException(ErrorCodeType errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

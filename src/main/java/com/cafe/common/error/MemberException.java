package com.cafe.common.error;

import lombok.Getter;

@Getter
public class MemberException extends BusinessException {
    // 회원/인증 도메인의 비즈니스 예외다.

    public MemberException(ErrorCodeType errorCode) {
        super(errorCode);
    }

    public MemberException(ErrorCodeType errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

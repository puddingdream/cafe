package com.cafe.common.error;

import org.springframework.http.HttpStatus;

public interface ErrorCodeType {

    // HTTP 응답 상태, 서비스 코드, 사용자 메시지를 예외 처리기에서 공통으로 사용한다.
    HttpStatus getStatus();

    String getCode();

    String getMessage();
}

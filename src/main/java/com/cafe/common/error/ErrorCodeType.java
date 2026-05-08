package com.cafe.common.error;

import org.springframework.http.HttpStatus;

public interface ErrorCodeType {

    HttpStatus getStatus();

    String getCode();

    String getMessage();
}

package com.cafe.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PointErrorCode implements ErrorCodeType {
    INVALID_CHARGE_POINT(HttpStatus.BAD_REQUEST, "P001", "충전 포인트는 0보다 커야 합니다."),
    POINT_WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "P002", "포인트 지갑을 찾을 수 없습니다."),
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "P003", "포인트가 부족합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

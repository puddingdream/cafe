package com.cafe.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCodeType {
    EMPTY_ORDER_ITEMS(HttpStatus.BAD_REQUEST, "O001", "주문할 메뉴를 선택해야 합니다."),
    INVALID_ORDER_QUANTITY(HttpStatus.BAD_REQUEST, "O002", "주문 수량은 0보다 커야 합니다."),
    ORDER_MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "O003", "주문 메뉴를 찾을 수 없습니다."),
    NOT_ORDERABLE_MENU(HttpStatus.BAD_REQUEST, "O004", "현재 주문할 수 없는 메뉴입니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O005", "주문을 찾을 수 없습니다."),
    FORBIDDEN_ORDER_ACCESS(HttpStatus.FORBIDDEN, "O006", "해당 주문에 접근할 수 없습니다."),
    NOT_CANCELABLE_ORDER(HttpStatus.BAD_REQUEST, "O007", "취소할 수 없는 주문입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

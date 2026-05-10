package com.cafe.domain.order.enums;

public enum OrderStatus {
    // PAID는 집계 대상, CANCELED는 환불 완료되어 집계에서 제외되는 상태다.
    PAID,
    CANCELED
}

package com.cafe.domain.point.dto;

import jakarta.validation.constraints.Positive;

public record PointChargeRequest(
        // 충전할 포인트 금액이다. 과제에서는 1원=1P로 본다.
        @Positive(message = "충전 포인트는 0보다 커야 합니다.")
        long chargePoint
) {
}

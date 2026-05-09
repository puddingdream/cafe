package com.cafe.domain.point.dto;

import jakarta.validation.constraints.Positive;

public record PointChargeRequest(
        @Positive(message = "충전 포인트는 0보다 커야 합니다.")
        long chargePoint
) {
}

package com.cafe.domain.point.dto;

public record PointChargeResponse(
        Long memberId,
        long chargePoint,
        long afterPoint
) {
}

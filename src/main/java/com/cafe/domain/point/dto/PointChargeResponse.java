package com.cafe.domain.point.dto;

public record PointChargeResponse(
        // 포인트 충전 결과와 충전 후 잔액을 반환한다.
        Long memberId,
        long chargePoint,
        long afterPoint
) {
}

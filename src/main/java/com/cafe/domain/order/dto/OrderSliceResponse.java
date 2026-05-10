package com.cafe.domain.order.dto;

import org.springframework.data.domain.Slice;

import java.util.List;

public record OrderSliceResponse(
        // 내 주문 목록 응답은 Slice 기반으로 다음 페이지 존재 여부만 제공한다.
        List<OrderGetResponse> orders,
        int page,
        int size,
        boolean hasNext
) {
    public static OrderSliceResponse from(Slice<OrderGetResponse> orders) {
        // Spring Slice를 클라이언트 응답 형태로 변환한다.
        return new OrderSliceResponse(
                orders.getContent(),
                orders.getNumber(),
                orders.getSize(),
                orders.hasNext()
        );
    }
}

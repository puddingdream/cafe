package com.cafe.domain.order.dto;

import org.springframework.data.domain.Slice;

import java.util.List;

public record OrderSliceResponse(
        List<OrderGetResponse> orders,
        int page,
        int size,
        boolean hasNext
) {
    public static OrderSliceResponse from(Slice<OrderGetResponse> orders) {
        return new OrderSliceResponse(
                orders.getContent(),
                orders.getNumber(),
                orders.getSize(),
                orders.hasNext()
        );
    }
}

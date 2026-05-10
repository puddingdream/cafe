package com.cafe.domain.order.dto;

import com.cafe.domain.order.entity.Order;
import com.cafe.domain.order.entity.OrderItem;
import com.cafe.domain.order.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderCancelResponse(
        // 주문 취소 결과와 환불 후 포인트를 함께 내려준다.
        Long orderId,
        String orderNumber,
        Long memberId,
        long refundPoint,
        long afterPoint,
        OrderStatus status,
        LocalDateTime orderedAt,
        List<OrderGetResponse.Item> items
) {
    public static OrderCancelResponse of(Order order, long afterPoint, List<OrderItem> orderItems) {
        // 취소된 주문과 상세 항목을 취소 응답으로 조립한다.
        return new OrderCancelResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getMemberId(),
                order.getTotalAmount(),
                afterPoint,
                order.getStatus(),
                order.getOrderedAt(),
                orderItems.stream()
                        .map(OrderGetResponse.Item::from)
                        .toList()
        );
    }
}

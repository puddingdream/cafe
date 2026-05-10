package com.cafe.domain.order.event;

import com.cafe.domain.order.entity.Order;
import com.cafe.domain.order.entity.OrderItem;

import java.time.LocalDateTime;
import java.util.List;

public record OrderCanceledEvent(
        Long orderId,
        String orderNumber,
        Long memberId,
        LocalDateTime orderedAt,
        List<OrderEventItem> items
) {
    public static OrderCanceledEvent of(Order order, List<OrderItem> orderItems) {
        return new OrderCanceledEvent(
                order.getId(),
                order.getOrderNumber(),
                order.getMemberId(),
                order.getOrderedAt(),
                orderItems.stream()
                        .map(OrderEventItem::from)
                        .toList()
        );
    }
}

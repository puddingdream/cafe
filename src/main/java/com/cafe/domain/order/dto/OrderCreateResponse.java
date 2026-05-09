package com.cafe.domain.order.dto;

import com.cafe.domain.order.entity.Order;
import com.cafe.domain.order.entity.OrderItem;
import com.cafe.domain.order.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderCreateResponse(
        Long orderId,
        String orderNumber,
        Long memberId,
        long totalAmount,
        long usedPoint,
        long afterPoint,
        OrderStatus status,
        LocalDateTime orderedAt,
        List<Item> items
) {
    public static OrderCreateResponse of(Order order, long afterPoint, List<OrderItem> orderItems) {
        return new OrderCreateResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getMemberId(),
                order.getTotalAmount(),
                order.getTotalAmount(),
                afterPoint,
                order.getStatus(),
                order.getOrderedAt(),
                orderItems.stream()
                        .map(Item::from)
                        .toList()
        );
    }

    public record Item(
            Long menuId,
            String menuName,
            long menuPrice,
            int quantity,
            long totalPrice
    ) {
        public static Item from(OrderItem orderItem) {
            return new Item(
                    orderItem.getMenuId(),
                    orderItem.getMenuName(),
                    orderItem.getMenuPrice(),
                    orderItem.getQuantity(),
                    orderItem.getTotalPrice()
            );
        }
    }
}

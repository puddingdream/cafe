package com.cafe.domain.order.dto;

import com.cafe.domain.order.entity.Order;
import com.cafe.domain.order.entity.OrderItem;
import com.cafe.domain.order.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderGetResponse(
        Long orderId,
        String orderNumber,
        Long memberId,
        long totalAmount,
        OrderStatus status,
        LocalDateTime orderedAt,
        List<Item> items
) {
    public static OrderGetResponse of(Order order, List<OrderItem> orderItems) {
        return new OrderGetResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getMemberId(),
                order.getTotalAmount(),
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

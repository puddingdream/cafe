package com.cafe.domain.order.event;

import com.cafe.domain.order.entity.OrderItem;

public record OrderEventItem(
        Long menuId,
        int quantity
) {
    public static OrderEventItem from(OrderItem orderItem) {
        return new OrderEventItem(
                orderItem.getMenuId(),
                orderItem.getQuantity()
        );
    }
}

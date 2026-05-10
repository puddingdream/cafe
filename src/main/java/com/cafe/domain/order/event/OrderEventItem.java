package com.cafe.domain.order.event;

import com.cafe.domain.order.entity.OrderItem;

public record OrderEventItem(
        // 주문 이벤트에서 인기 메뉴 집계에 필요한 메뉴별 수량 스냅샷이다.
        Long menuId,
        int quantity
) {
    public static OrderEventItem from(OrderItem orderItem) {
        // 메뉴명/가격은 랭킹 계산에 필요 없으므로 menuId와 quantity만 담는다.
        return new OrderEventItem(
                orderItem.getMenuId(),
                orderItem.getQuantity()
        );
    }
}

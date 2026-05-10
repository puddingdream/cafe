package com.cafe.domain.order.event;

import com.cafe.domain.order.entity.Order;
import com.cafe.domain.order.entity.OrderItem;

import java.time.LocalDateTime;
import java.util.List;

public record OrderCanceledEvent(
        // 주문 취소 후 Redis 인기 메뉴 점수를 되돌리기 위한 이벤트 payload다.
        Long orderId,
        String orderNumber,
        Long memberId,
        LocalDateTime orderedAt,
        List<OrderEventItem> items
) {
    public static OrderCanceledEvent of(Order order, List<OrderItem> orderItems) {
        // 주문 취소 이벤트도 원 주문일자를 기준으로 점수를 차감해야 하므로 orderedAt을 포함한다.
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

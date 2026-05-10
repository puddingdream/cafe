package com.cafe.domain.order.event;

import com.cafe.domain.order.entity.Order;
import com.cafe.domain.order.entity.OrderItem;

import java.time.LocalDateTime;
import java.util.List;

public record OrderPaidEvent(
        // 주문 완료 후 Kafka로 전달할 이벤트 payload다.
        Long orderId,
        String orderNumber,
        Long memberId,
        LocalDateTime orderedAt,
        List<OrderEventItem> items
) {
    public static OrderPaidEvent of(Order order, List<OrderItem> orderItems) {
        // 주문 엔티티와 상세 엔티티를 이벤트 전송에 필요한 값으로 변환한다.
        return new OrderPaidEvent(
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

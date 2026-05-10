package com.cafe.domain.order.dto;

import com.cafe.domain.order.entity.Order;
import com.cafe.domain.order.entity.OrderItem;
import com.cafe.domain.order.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderCreateResponse(
        // 주문 생성 성공 후 주문/결제 결과를 함께 내려준다.
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
        // 주문 엔티티와 저장된 주문 상세를 생성 응답으로 조립한다.
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
            // 주문 시점의 메뉴별 스냅샷 정보다.
            Long menuId,
            String menuName,
            long menuPrice,
            int quantity,
            long totalPrice
    ) {
        public static Item from(OrderItem orderItem) {
            // OrderItem 엔티티를 응답용 상세 항목으로 변환한다.
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

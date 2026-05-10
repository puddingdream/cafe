package com.cafe.domain.order.dto;

import com.cafe.domain.order.entity.Order;
import com.cafe.domain.order.entity.OrderItem;
import com.cafe.domain.order.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderGetResponse(
        // 주문 단건/목록 조회에서 사용하는 주문 상세 응답이다.
        Long orderId,
        String orderNumber,
        Long memberId,
        long totalAmount,
        OrderStatus status,
        LocalDateTime orderedAt,
        List<Item> items
) {
    public static OrderGetResponse of(Order order, List<OrderItem> orderItems) {
        // 주문과 주문 상세 목록을 조회 응답으로 조립한다.
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
            // 주문 당시 저장된 메뉴 스냅샷이다.
            Long menuId,
            String menuName,
            long menuPrice,
            int quantity,
            long totalPrice
    ) {
        public static Item from(OrderItem orderItem) {
            // OrderItem 엔티티를 조회 응답 항목으로 변환한다.
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

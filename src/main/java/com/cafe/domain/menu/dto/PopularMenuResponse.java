package com.cafe.domain.menu.dto;

import com.cafe.domain.menu.entity.Menu;

public record PopularMenuResponse(
        // 인기 메뉴 응답은 메뉴 기본 정보에 최근 7일 주문 수량을 추가한다.
        Long id,
        String name,
        String description,
        String imageUrl,
        int price,
        String category,
        String categoryLabel,
        String status,
        long orderCount
) {
    public static PopularMenuResponse from(Menu menu, long orderCount) {
        // 메뉴 엔티티와 집계 수량을 하나의 응답으로 조립한다.
        return new PopularMenuResponse(
                menu.getId(),
                menu.getName(),
                menu.getDescription(),
                menu.getImageUrl(),
                menu.getPrice(),
                menu.getCategory().name(),
                menu.getCategory().getLabel(),
                menu.getStatus().name(),
                orderCount
        );
    }
}

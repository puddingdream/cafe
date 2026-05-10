package com.cafe.domain.menu.dto;

import com.cafe.domain.menu.entity.Menu;

public record PopularMenuResponse(
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

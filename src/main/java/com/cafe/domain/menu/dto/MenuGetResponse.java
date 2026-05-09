package com.cafe.domain.menu.dto;

import com.cafe.domain.menu.entity.Menu;

public record MenuGetResponse(
        Long id,
        String name,
        String description,
        String imageUrl,
        int price,
        String category,
        String categoryLabel,
        String status
) {
    public static MenuGetResponse from(Menu menu) {
        return new MenuGetResponse(
                menu.getId(),
                menu.getName(),
                menu.getDescription(),
                menu.getImageUrl(),
                menu.getPrice(),
                menu.getCategory().name(),
                menu.getCategory().getLabel(),
                menu.getStatus().name()
        );
    }
}

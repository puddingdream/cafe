package com.cafe.domain.menu.dto;

import com.cafe.domain.menu.entity.Menu;
import com.cafe.domain.menu.enums.MenuStatus;

public record MenuCreateResponse(
        Long id,
        String name,
        String description,
        String imageUrl,
        int price,
        String category,
        String categoryLabel,
        MenuStatus status
) {
    public static MenuCreateResponse from(Menu menu) {
        return new MenuCreateResponse(
                menu.getId(),
                menu.getName(),
                menu.getDescription(),
                menu.getImageUrl(),
                menu.getPrice(),
                menu.getCategory().name(),
                menu.getCategory().getLabel(),
                menu.getStatus()
        );
    }
}

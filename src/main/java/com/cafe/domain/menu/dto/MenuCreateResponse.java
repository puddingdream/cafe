package com.cafe.domain.menu.dto;

import com.cafe.domain.menu.entity.Menu;
import com.cafe.domain.menu.enums.MenuStatus;

public record MenuCreateResponse(
        // 메뉴 생성 후 클라이언트에 내려주는 메뉴 기본 정보다.
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
        // Menu 엔티티를 생성 응답 DTO로 변환한다.
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

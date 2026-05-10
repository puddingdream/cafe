package com.cafe.domain.menu.dto;

import com.cafe.domain.menu.entity.Menu;

public record MenuGetResponse(
        // 메뉴 목록/수정/상태 변경 응답에서 공통으로 사용하는 메뉴 정보다.
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
        // Menu 엔티티를 조회 응답 DTO로 변환한다.
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

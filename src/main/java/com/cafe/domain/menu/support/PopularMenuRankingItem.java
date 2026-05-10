package com.cafe.domain.menu.support;

public record PopularMenuRankingItem(
        Long menuId,
        long orderCount
) {
}

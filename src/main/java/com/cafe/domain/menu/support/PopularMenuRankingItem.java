package com.cafe.domain.menu.support;

public record PopularMenuRankingItem(
        // Redis ZSET에서 계산한 메뉴별 주문 수량 랭킹 항목이다.
        Long menuId,
        long orderCount
) {
}

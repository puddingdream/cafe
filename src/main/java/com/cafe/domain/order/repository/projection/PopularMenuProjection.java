package com.cafe.domain.order.repository.projection;

import com.cafe.domain.menu.entity.Menu;

public record PopularMenuProjection(
        // V1 인기 메뉴 쿼리 결과를 메뉴 객체와 주문 수량으로 받는 projection이다.
        Menu menu,
        Long orderCount
) {
}

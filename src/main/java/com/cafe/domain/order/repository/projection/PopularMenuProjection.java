package com.cafe.domain.order.repository.projection;

import com.cafe.domain.menu.entity.Menu;

public record PopularMenuProjection(
        Menu menu,
        Long orderCount
) {
}

package com.cafe.domain.menu.support;

import com.cafe.domain.order.event.OrderCanceledEvent;
import com.cafe.domain.order.event.OrderEventItem;
import com.cafe.domain.order.event.OrderPaidEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.dummy-data.enabled=false")
class PopularMenuRankingServiceIntegrationTest {

    @Autowired
    private PopularMenuRankingService popularMenuRankingService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.delete("popular:menus:" + LocalDate.now());
    }

    @Test
    void increaseAndDecreaseRankingScore() {
        LocalDateTime orderedAt = LocalDateTime.now();

        popularMenuRankingService.increase(new OrderPaidEvent(
                1L,
                "ORDER-1",
                1L,
                orderedAt,
                List.of(
                        new OrderEventItem(10_001L, 2),
                        new OrderEventItem(10_002L, 5)
                )
        ));

        List<PopularMenuRankingItem> increasedRankings = popularMenuRankingService.findTopMenus(2);
        assertThat(increasedRankings).containsExactly(
                new PopularMenuRankingItem(10_002L, 5L),
                new PopularMenuRankingItem(10_001L, 2L)
        );

        popularMenuRankingService.decrease(new OrderCanceledEvent(
                1L,
                "ORDER-1",
                1L,
                orderedAt,
                List.of(new OrderEventItem(10_002L, 3))
        ));

        List<PopularMenuRankingItem> decreasedRankings = popularMenuRankingService.findTopMenus(2);
        assertThat(decreasedRankings).containsExactly(
                new PopularMenuRankingItem(10_001L, 2L),
                new PopularMenuRankingItem(10_002L, 2L)
        );
    }
}

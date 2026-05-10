package com.cafe.domain.menu.support;

import com.cafe.domain.order.event.OrderCanceledEvent;
import com.cafe.domain.order.event.OrderEventItem;
import com.cafe.domain.order.event.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularMenuRankingService {
    // Kafka 주문 이벤트를 Redis ZSET 점수로 반영하고 최근 7일 랭킹을 계산한다.

    private static final String DAILY_KEY_PREFIX = "popular:menus:";
    private static final DateTimeFormatter KEY_DATE_FORMATTER = DateTimeFormatter.ISO_DATE;
    private static final int POPULAR_MENU_DAYS = 7;
    private static final Duration DAILY_KEY_TTL = Duration.ofDays(8);

    private final StringRedisTemplate stringRedisTemplate;

    public void increase(OrderPaidEvent event) {
        // 주문 완료 이벤트는 주문 수량만큼 해당 날짜 ZSET 점수를 올린다.
        String key = getDailyKey(event.orderedAt().toLocalDate());
        updateScore(key, event.items(), 1L);
        stringRedisTemplate.expire(key, DAILY_KEY_TTL);
    }

    public void decrease(OrderCanceledEvent event) {
        // 주문 취소 이벤트는 원 주문일자의 점수를 내린다. key가 이미 만료됐으면 보정할 대상이 없으므로 무시한다.
        String key = getDailyKey(event.orderedAt().toLocalDate());
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            return;
        }

        updateScore(key, event.items(), -1L);
        stringRedisTemplate.expire(key, DAILY_KEY_TTL);
    }

    public List<PopularMenuRankingItem> findTopMenus(int limit) {
        // 현재 구조는 rolling key 없이 최근 7일 일별 ZSET을 조회 시점에 합산한다.
        Map<Long, Long> orderCountByMenuId = new HashMap<>();
        LocalDate today = LocalDate.now();

        for (int daysAgo = 0; daysAgo < POPULAR_MENU_DAYS; daysAgo++) {
            String key = getDailyKey(today.minusDays(daysAgo));
            Set<ZSetOperations.TypedTuple<String>> tuples =
                    stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, 0, -1);

            if (tuples == null || tuples.isEmpty()) {
                continue;
            }

            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                Long menuId = parseMenuId(tuple.getValue());
                Double score = tuple.getScore();
                if (menuId == null || score == null || score <= 0) {
                    continue;
                }
                orderCountByMenuId.merge(menuId, Math.round(score), Long::sum);
            }
        }

        return orderCountByMenuId.entrySet()
                .stream()
                .sorted(Comparator.<Map.Entry<Long, Long>>comparingLong(Map.Entry::getValue)
                        .reversed()
                        .thenComparingLong(Map.Entry::getKey))
                .limit(limit)
                .map(entry -> new PopularMenuRankingItem(entry.getKey(), entry.getValue()))
                .toList();
    }

    private void updateScore(String key, List<OrderEventItem> items, long direction) {
        // direction=1이면 주문 반영, -1이면 취소 반영이다.
        for (OrderEventItem item : items) {
            long delta = item.quantity() * direction;
            String menuId = item.menuId().toString();
            Double score = stringRedisTemplate.opsForZSet().incrementScore(key, menuId, delta);

            if (score != null && score <= 0) {
                stringRedisTemplate.opsForZSet().remove(key, menuId);
            }
        }
    }

    private String getDailyKey(LocalDate date) {
        return DAILY_KEY_PREFIX + date.format(KEY_DATE_FORMATTER);
    }

    private Long parseMenuId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            log.warn("Invalid menu id in popular menu ranking. value={}", value);
            return null;
        }
    }
}

package com.cafe.performance;

import com.cafe.domain.menu.entity.Menu;
import com.cafe.domain.menu.enums.MenuCategory;
import com.cafe.domain.menu.repository.MenuRepository;
import com.cafe.domain.menu.service.MenuService;
import com.cafe.domain.order.entity.Order;
import com.cafe.domain.order.entity.OrderItem;
import com.cafe.domain.order.repository.OrderItemRepository;
import com.cafe.domain.order.repository.OrderRepository;
import com.cafe.domain.order.support.OrderStatisticsReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.dummy-data.enabled=false",
        "spring.data.redis.database=1"
})
@EnabledIfSystemProperty(named = "performance", matches = "true")
class PopularMenuPerformanceTest {

    private static final int MENU_COUNT = 20;
    private static final int ORDER_COUNT = 5_000;
    private static final int ITEMS_PER_ORDER = 3;
    private static final int WARM_UP_COUNT = 5;
    private static final int MEASURE_COUNT = 50;
    private static final String DAILY_RANKING_KEY_PREFIX = "popular:menus:";

    @Autowired
    private OrderStatisticsReader orderStatisticsReader;

    @Autowired
    private MenuService menuService;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void comparePopularMenuV1RdbAndV2RedisZSet() throws IOException {
        String runId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String orderNumberPrefix = "PERF-" + runId + "-";
        String menuNamePrefix = "Perf Menu " + runId + " ";
        LocalDateTime orderedFrom = LocalDateTime.now().minusSeconds(1);
        String todayRankingKey = DAILY_RANKING_KEY_PREFIX + LocalDate.now();

        try {
            TestData testData = createTestData(menuNamePrefix, orderNumberPrefix);
            populateRedisRanking(todayRankingKey, testData.orderCountByMenuId());

            runWarmUp(orderedFrom);

            MeasurementResult v1Result = measure("V1 RDB", () -> {
                assertThat(orderStatisticsReader.findPopularMenus(orderedFrom)).hasSize(3);
            });
            MeasurementResult v2Result = measure("V2 Redis ZSET", () -> {
                assertThat(menuService.getPopularMenusV2()).hasSize(3);
            });

            writeReport(runId, v1Result, v2Result);
        } finally {
            cleanup(orderNumberPrefix, menuNamePrefix, todayRankingKey);
        }
    }

    private TestData createTestData(String menuNamePrefix, String orderNumberPrefix) {
        List<Menu> menus = createMenus(menuNamePrefix);
        List<Order> orders = createOrders(orderNumberPrefix, menus);
        Map<Long, Long> orderCountByMenuId = createOrderItems(menus, orders);

        return new TestData(orderCountByMenuId);
    }

    private List<Menu> createMenus(String menuNamePrefix) {
        List<Menu> menus = new ArrayList<>();
        for (int index = 0; index < MENU_COUNT; index++) {
            menus.add(Menu.builder()
                    .name(menuNamePrefix + index)
                    .description("Performance test menu " + index)
                    .price(4_000 + (index * 100))
                    .category(MenuCategory.COFFEE)
                    .imageUrl("https://example.com/perf-menu-" + index + ".png")
                    .build());
        }
        return menuRepository.saveAll(menus).stream()
                .sorted(Comparator.comparing(Menu::getId))
                .toList();
    }

    private List<Order> createOrders(String orderNumberPrefix, List<Menu> menus) {
        List<Order> orders = new ArrayList<>();
        for (int index = 0; index < ORDER_COUNT; index++) {
            long totalAmount = calculateTotalAmount(menus, index);
            orders.add(Order.builder()
                    .orderNumber(orderNumberPrefix + index)
                    .memberId((long) (index % 100) + 1)
                    .totalAmount(totalAmount)
                    .build());
        }
        return orderRepository.saveAll(orders);
    }

    private Map<Long, Long> createOrderItems(List<Menu> menus, List<Order> orders) {
        List<OrderItem> orderItems = new ArrayList<>(ORDER_COUNT * ITEMS_PER_ORDER);
        Map<Long, Long> orderCountByMenuId = new HashMap<>();

        for (int orderIndex = 0; orderIndex < orders.size(); orderIndex++) {
            Order order = orders.get(orderIndex);
            for (int itemIndex = 0; itemIndex < ITEMS_PER_ORDER; itemIndex++) {
                Menu menu = selectMenu(menus, orderIndex, itemIndex);
                int quantity = selectQuantity(orderIndex, itemIndex);
                orderItems.add(OrderItem.builder()
                        .orderId(order.getId())
                        .menuId(menu.getId())
                        .menuName(menu.getName())
                        .menuPrice(menu.getPrice())
                        .quantity(quantity)
                        .build());
                orderCountByMenuId.merge(menu.getId(), (long) quantity, Long::sum);
            }
        }

        orderItemRepository.saveAll(orderItems);
        return orderCountByMenuId;
    }

    private void populateRedisRanking(String key, Map<Long, Long> orderCountByMenuId) {
        stringRedisTemplate.delete(key);
        orderCountByMenuId.forEach((menuId, orderCount) ->
                stringRedisTemplate.opsForZSet().add(key, menuId.toString(), orderCount.doubleValue()));
        stringRedisTemplate.expire(key, Duration.ofDays(8));
    }

    private void runWarmUp(LocalDateTime orderedFrom) {
        for (int count = 0; count < WARM_UP_COUNT; count++) {
            orderStatisticsReader.findPopularMenus(orderedFrom);
            menuService.getPopularMenusV2();
        }
    }

    private MeasurementResult measure(String name, Runnable runnable) {
        List<Long> elapsedTimes = new ArrayList<>();
        for (int count = 0; count < MEASURE_COUNT; count++) {
            long start = System.nanoTime();
            runnable.run();
            elapsedTimes.add(System.nanoTime() - start);
        }

        return MeasurementResult.from(name, elapsedTimes);
    }

    private void writeReport(String runId, MeasurementResult v1Result, MeasurementResult v2Result) throws IOException {
        Path reportPath = Path.of("build", "reports", "performance", "popular-menu-performance.md");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, """
                # Popular Menu Performance Test

                - runId: %s
                - menus: %d
                - orders: %,d
                - order items: %,d
                - warm up: %d
                - measured iterations: %d

                | target | avg ms | min ms | max ms | p95 ms |
                |---|---:|---:|---:|---:|
                | %s | %.3f | %.3f | %.3f | %.3f |
                | %s | %.3f | %.3f | %.3f | %.3f |

                Note: V1 measures RDB aggregation query. V2 measures Redis ZSET ranking lookup plus Top3 menu lookup/response composition.
                """.formatted(
                runId,
                MENU_COUNT,
                ORDER_COUNT,
                ORDER_COUNT * ITEMS_PER_ORDER,
                WARM_UP_COUNT,
                MEASURE_COUNT,
                v1Result.name(), v1Result.avgMs(), v1Result.minMs(), v1Result.maxMs(), v1Result.p95Ms(),
                v2Result.name(), v2Result.avgMs(), v2Result.minMs(), v2Result.maxMs(), v2Result.p95Ms()
        ));
    }

    private void cleanup(String orderNumberPrefix, String menuNamePrefix, String todayRankingKey) {
        stringRedisTemplate.delete(todayRankingKey);
        jdbcTemplate.update("""
                delete oi
                from order_items oi
                join orders o on o.id = oi.order_id
                where o.order_number like ?
                """, orderNumberPrefix + "%");
        jdbcTemplate.update("delete from orders where order_number like ?", orderNumberPrefix + "%");
        jdbcTemplate.update("delete from menus where name like ?", menuNamePrefix + "%");
    }

    private long calculateTotalAmount(List<Menu> menus, int orderIndex) {
        long totalAmount = 0L;
        for (int itemIndex = 0; itemIndex < ITEMS_PER_ORDER; itemIndex++) {
            Menu menu = selectMenu(menus, orderIndex, itemIndex);
            totalAmount += (long) menu.getPrice() * selectQuantity(orderIndex, itemIndex);
        }
        return totalAmount;
    }

    private Menu selectMenu(List<Menu> menus, int orderIndex, int itemIndex) {
        return menus.get(Math.floorMod(orderIndex + itemIndex * 7, menus.size()));
    }

    private int selectQuantity(int orderIndex, int itemIndex) {
        return Math.floorMod(orderIndex + itemIndex, 5) + 1;
    }

    private record TestData(Map<Long, Long> orderCountByMenuId) {
    }

    private record MeasurementResult(
            String name,
            double avgMs,
            double minMs,
            double maxMs,
            double p95Ms
    ) {
        static MeasurementResult from(String name, List<Long> elapsedTimes) {
            List<Long> sortedTimes = elapsedTimes.stream()
                    .sorted()
                    .toList();
            double avgMs = elapsedTimes.stream()
                    .mapToDouble(MeasurementResult::toMillis)
                    .average()
                    .orElse(0D);
            double minMs = toMillis(sortedTimes.getFirst());
            double maxMs = toMillis(sortedTimes.getLast());
            int p95Index = Math.min(sortedTimes.size() - 1, (int) Math.ceil(sortedTimes.size() * 0.95) - 1);
            double p95Ms = toMillis(sortedTimes.get(p95Index));

            return new MeasurementResult(name, avgMs, minMs, maxMs, p95Ms);
        }

        private static double toMillis(long nanos) {
            return nanos / 1_000_000D;
        }
    }
}

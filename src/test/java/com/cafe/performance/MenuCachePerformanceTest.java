package com.cafe.performance;

import com.cafe.domain.menu.entity.Menu;
import com.cafe.domain.menu.enums.MenuCategory;
import com.cafe.domain.menu.repository.MenuRepository;
import com.cafe.domain.menu.service.MenuService;
import com.cafe.infrastructure.redis.CacheNames;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.dummy-data.enabled=false",
        "spring.data.redis.database=1"
})
@EnabledIfSystemProperty(named = "performance", matches = "true")
class MenuCachePerformanceTest {

    private static final int MENU_COUNT = 200;
    private static final int WARM_UP_COUNT = 5;
    private static final int MEASURE_COUNT = 100;

    @Autowired
    private MenuService menuService;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void compareCategoryMenuCacheMissAndHit() throws IOException {
        String runId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String menuNamePrefix = "Menu Cache Perf " + runId + " ";

        try {
            createMenus(menuNamePrefix);
            runWarmUp();

            MeasurementResult cacheMissResult = measure("Cache miss", () -> {
                clearMenusCache();
                assertThat(menuService.getMenus("COFFEE")).isNotEmpty();
            });

            clearMenusCache();
            menuService.getMenus("COFFEE");
            MeasurementResult cacheHitResult = measure("Cache hit", () -> {
                assertThat(menuService.getMenus("COFFEE")).isNotEmpty();
            });

            writeReport(runId, cacheMissResult, cacheHitResult);
        } finally {
            clearMenusCache();
            cleanup(menuNamePrefix);
        }
    }

    private void createMenus(String menuNamePrefix) {
        List<Menu> menus = new ArrayList<>();
        for (int index = 0; index < MENU_COUNT; index++) {
            menus.add(Menu.builder()
                    .name(menuNamePrefix + index)
                    .description("Menu cache performance test menu " + index)
                    .price(4_000 + index)
                    .category(MenuCategory.COFFEE)
                    .imageUrl("https://example.com/menu-cache-perf-" + index + ".png")
                    .build());
        }
        menuRepository.saveAll(menus);
    }

    private void runWarmUp() {
        for (int count = 0; count < WARM_UP_COUNT; count++) {
            clearMenusCache();
            menuService.getMenus("COFFEE");
            menuService.getMenus("COFFEE");
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

    private void writeReport(String runId, MeasurementResult cacheMissResult, MeasurementResult cacheHitResult)
            throws IOException {
        Path reportPath = Path.of("build", "reports", "performance", "menu-cache-performance.md");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, """
                # Menu Cache Performance Test

                - runId: %s
                - menus inserted for this run: %,d
                - warm up: %d
                - measured iterations: %d

                | target | avg ms | min ms | max ms | p95 ms |
                |---|---:|---:|---:|---:|
                | %s | %.3f | %.3f | %.3f | %.3f |
                | %s | %.3f | %.3f | %.3f | %.3f |

                Note: Cache miss includes DB 조회, 응답 조립, Redis cache write. Cache hit reads the cached category menu response from Redis Cache.
                """.formatted(
                runId,
                MENU_COUNT,
                WARM_UP_COUNT,
                MEASURE_COUNT,
                cacheMissResult.name(), cacheMissResult.avgMs(), cacheMissResult.minMs(),
                cacheMissResult.maxMs(), cacheMissResult.p95Ms(),
                cacheHitResult.name(), cacheHitResult.avgMs(), cacheHitResult.minMs(),
                cacheHitResult.maxMs(), cacheHitResult.p95Ms()
        ));
    }

    private void clearMenusCache() {
        Cache cache = cacheManager.getCache(CacheNames.MENUS);
        if (cache != null) {
            cache.clear();
        }
    }

    private void cleanup(String menuNamePrefix) {
        jdbcTemplate.update("delete from menus where name like ?", menuNamePrefix + "%");
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

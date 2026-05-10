package com.cafe.domain.menu.service;

import com.cafe.domain.member.entity.Member;
import com.cafe.domain.member.enums.MemberRole;
import com.cafe.domain.member.repository.MemberRepository;
import com.cafe.domain.menu.entity.Menu;
import com.cafe.domain.menu.enums.MenuCategory;
import com.cafe.domain.menu.repository.MenuRepository;
import com.cafe.infrastructure.redis.CacheNames;
import com.cafe.infrastructure.security.dto.LoginUserInfoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.dummy-data.enabled=false")
class MenuServiceCacheIntegrationTest {

    @Autowired
    private MenuService menuService;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        Cache cache = cacheManager.getCache(CacheNames.MENUS);
        if (cache != null) {
            cache.clear();
        }
        stringRedisTemplate.delete(menuCategoryCacheKey());
    }

    @Test
    void getMenusCachesByCategoryAndToggleEvictsMenusCache() throws InterruptedException {
        Menu menu = createMenu();
        Member admin = createAdmin();
        Cache cache = cacheManager.getCache(CacheNames.MENUS);
        assertThat(cache).isNotNull();

        menuService.getMenus("coffee");

        assertThat(stringRedisTemplate.hasKey(menuCategoryCacheKey())).isTrue();

        menuService.toggleMenuStatus(menu.getId(), LoginUserInfoDto.builder()
                .id(admin.getId())
                .build());

        assertEventuallyCacheKeyMissing();
    }

    private Menu createMenu() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return menuRepository.save(Menu.builder()
                .name("Cache Americano " + suffix)
                .description("Cache test menu")
                .price(4_000)
                .category(MenuCategory.COFFEE)
                .imageUrl("https://example.com/cache-" + suffix + ".png")
                .build());
    }

    private Member createAdmin() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return memberRepository.save(Member.builder()
                .email("admin-cache-" + suffix + "@cafe.test")
                .password("password")
                .name("Admin Cache")
                .phoneNumber("010-admin-" + suffix)
                .role(MemberRole.ADMIN)
                .build());
    }

    private String menuCategoryCacheKey() {
        return "cache:" + CacheNames.MENUS + "::category:COFFEE";
    }

    private void assertEventuallyCacheKeyMissing() throws InterruptedException {
        for (int count = 0; count < 10; count++) {
            if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(menuCategoryCacheKey()))) {
                return;
            }
            Thread.sleep(100);
        }

        assertThat(stringRedisTemplate.hasKey(menuCategoryCacheKey())).isFalse();
    }
}

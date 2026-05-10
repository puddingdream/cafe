package com.cafe.infrastructure.redis;

public final class CacheNames {
    // @Cacheable/@CacheEvict에서 사용하는 cache name을 한 곳에서 관리한다.
    public static final String MENUS = "menus";
    public static final String POPULAR_MENUS = "popularMenus";

    private CacheNames() {
    }
}

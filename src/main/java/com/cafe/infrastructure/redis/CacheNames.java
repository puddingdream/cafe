package com.cafe.infrastructure.redis;

public final class CacheNames {
    // @Cacheable/@CacheEvict에서 사용하는 cache name을 한 곳에서 관리한다.
    // 문자열을 여러 서비스에 직접 흩뿌리지 않기 위한 상수 클래스다.
    public static final String MENUS = "menus";
    public static final String POPULAR_MENUS = "popularMenus";

    private CacheNames() {
    }
}

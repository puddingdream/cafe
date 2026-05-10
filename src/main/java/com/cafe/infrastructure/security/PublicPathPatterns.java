package com.cafe.infrastructure.security;

public final class PublicPathPatterns {
    // SecurityConfig와 JwtAuthenticationFilter가 함께 사용하는 공개 경로 목록이다.
    public static final String[] ANY_METHOD = {
            "/error",
            "/favicon.ico",
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    public static final String[] PUBLIC_POST = {
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/reissue"
    };

    public static final String[] PUBLIC_GET = {
            "/api/menus",
            "/api/menus/popular",
            "/api/menus/popular/v2"
    };

    private PublicPathPatterns() {
    }
}

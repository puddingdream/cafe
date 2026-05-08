package com.cafe.infrastructure.security;

public final class PublicPathPatterns {
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
            "/api/menus/popular"
    };

    private PublicPathPatterns() {
    }
}

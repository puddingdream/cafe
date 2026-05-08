package com.cafe.domain.auth.dto;

public record LoginResponse(
        AuthTokens tokens,
        MemberInfo member
) {
    public record MemberInfo(
            Long id,
            String email,
            String name
    ) {
    }
}

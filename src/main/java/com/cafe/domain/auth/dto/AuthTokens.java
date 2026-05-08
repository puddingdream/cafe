package com.cafe.domain.auth.dto;

public record AuthTokens(
        String accessToken,
        String refreshToken
) {
}

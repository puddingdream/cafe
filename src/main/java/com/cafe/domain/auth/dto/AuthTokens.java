package com.cafe.domain.auth.dto;

public record AuthTokens(
        // API 인증에 사용할 짧은 수명의 token이다.
        String accessToken,
        // access token 재발급에 사용할 긴 수명의 token이다.
        String refreshToken
) {
}

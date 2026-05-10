package com.cafe.domain.auth.dto;

public record LoginResponse(
        // 로그인 성공 후 발급된 토큰 묶음이다.
        AuthTokens tokens,
        // 화면 표시와 후속 처리에 필요한 최소 회원 정보다.
        MemberInfo member
) {
    public record MemberInfo(
            // access token의 subject와 같은 회원 ID다.
            Long id,
            String email,
            String name
    ) {
    }
}

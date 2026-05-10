package com.cafe.domain.auth.dto;

public record SignUpResponse(
        // 회원가입 성공 후 생성된 회원 정보를 최소한으로 반환한다.
        Long memberId,
        String email,
        String message
) {
}

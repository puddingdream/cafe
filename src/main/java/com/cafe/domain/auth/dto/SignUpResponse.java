package com.cafe.domain.auth.dto;

public record SignUpResponse(
        Long memberId,
        String email,
        String message
) {
}

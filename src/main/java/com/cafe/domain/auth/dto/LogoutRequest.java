package com.cafe.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        // 서버 저장소에서 삭제할 refresh token이다.
        @NotBlank(message = "리프레시 토큰은 필수입니다.")
        String refreshToken
) {
}

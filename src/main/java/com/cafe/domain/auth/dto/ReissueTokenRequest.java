package com.cafe.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ReissueTokenRequest(
        // 새 access/refresh token을 발급받기 위한 refresh token이다.
        @NotBlank(message = "리프레시 토큰은 필수입니다.")
        String refreshToken
) {
}

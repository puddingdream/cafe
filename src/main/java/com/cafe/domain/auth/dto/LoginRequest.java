package com.cafe.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        // 로그인 사용자를 찾기 위한 이메일이다.
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        // BCrypt로 저장된 비밀번호와 비교할 평문 비밀번호다.
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}

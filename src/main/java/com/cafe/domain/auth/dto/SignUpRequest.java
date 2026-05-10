package com.cafe.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignUpRequest(
        // 회원을 유일하게 식별하는 로그인 이메일이다.
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        // 특수문자는 선택이며, 영문과 숫자를 포함한 8자 이상이면 허용한다.
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$",
                message = "비밀번호는 8자 이상이며 영문과 숫자를 포함해야 합니다."
        )
        String password,

        // 서비스 화면에 표시할 사용자 이름이다.
        @NotBlank(message = "이름은 필수입니다.")
        String name,

        // 중복 가입 방지를 위해 unique로 관리하는 전화번호다.
        @NotBlank(message = "전화번호는 필수입니다.")
        String phoneNumber
) {
}

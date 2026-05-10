package com.cafe.infrastructure.security.dto;

import lombok.Builder;

@Builder
public record LoginUserInfoDto(
        // 컨트롤러와 서비스에서 현재 로그인 사용자를 식별하기 위한 최소 정보다.
        Long id
) {
}

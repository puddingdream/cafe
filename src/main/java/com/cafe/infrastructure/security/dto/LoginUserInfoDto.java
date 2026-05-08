package com.cafe.infrastructure.security.dto;

import lombok.Builder;

@Builder
public record LoginUserInfoDto(
        Long id
) {
}

package com.cafe.domain.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.web.multipart.MultipartFile;

public record MenuUpdateRequest(
        @NotBlank(message = "메뉴 이름은 필수입니다.")
        String name,

        @NotBlank(message = "메뉴 설명은 필수입니다.")
        String description,

        @Positive(message = "가격은 0원보다 커야합니다.")
        int price,

        @NotBlank(message = "메뉴 카테고리는 필수입니다.")
        String category,

        MultipartFile imageFile
) {
}

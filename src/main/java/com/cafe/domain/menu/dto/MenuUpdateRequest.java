package com.cafe.domain.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.web.multipart.MultipartFile;

public record MenuUpdateRequest(
        // 수정할 메뉴 이름이다.
        @NotBlank(message = "메뉴 이름은 필수입니다.")
        String name,

        // 수정할 메뉴 설명이다.
        @NotBlank(message = "메뉴 설명은 필수입니다.")
        String description,

        // 수정할 메뉴 가격이다.
        @Positive(message = "가격은 0원보다 커야합니다.")
        int price,

        // 수정할 메뉴 카테고리다.
        @NotBlank(message = "메뉴 카테고리는 필수입니다.")
        String category,

        // 수정에서는 이미지가 없으면 기존 이미지를 유지한다.
        MultipartFile imageFile
) {
}

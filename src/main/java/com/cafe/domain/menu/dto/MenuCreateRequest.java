package com.cafe.domain.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.multipart.MultipartFile;

public record MenuCreateRequest(
        // 신규 메뉴 이름이다.
        @NotBlank(message = "메뉴 이름은 필수입니다.")
        String name,

        // 메뉴 설명 문구다.
        @NotBlank(message = "메뉴 설명은 필수입니다.")
        String description,

        // 포인트 결제 금액과 동일하게 쓰이는 메뉴 가격이다.
        @Positive(message = "가격은 0원보다 커야합니다.")
        int price,

        // enum 이름 또는 한글 라벨로 들어온 카테고리 값이다.
        @NotBlank(message = "메뉴 카테고리는 필수입니다.")
        String category,

        // 생성 시 메뉴 이미지는 필수다.
        @NotNull(message = "메뉴 이미지는 필수입니다.")
        MultipartFile imageFile
) {
}

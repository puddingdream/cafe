package com.cafe.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record OrderCreateRequest(
        @NotEmpty(message = "주문할 메뉴를 선택해야 합니다.")
        List<@Valid Item> items
) {
    public record Item(
            @NotNull(message = "메뉴 ID는 필수입니다.")
            Long menuId,

            @Positive(message = "주문 수량은 0보다 커야 합니다.")
            int quantity
    ) {
    }
}

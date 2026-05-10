package com.cafe.domain.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Getter
@Setter
public class OrderSearchRequest {

    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    private int page = 0;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 50, message = "페이지 크기는 최대 50까지 가능합니다.")
    private int size = 20;

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}

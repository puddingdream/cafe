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

    // 주문 목록 페이지 번호다. Spring PageRequest 기준 0부터 시작한다.
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    private int page = 0;

    // 한 번에 가져올 주문 수다. 과도한 조회를 막기 위해 최대 50으로 제한한다.
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 50, message = "페이지 크기는 최대 50까지 가능합니다.")
    private int size = 20;

    public Pageable toPageable() {
        // 컨트롤러에서 받은 검색 조건을 JPA Pageable로 변환한다.
        return PageRequest.of(page, size);
    }
}

package com.cafe.domain.order.support;

import com.cafe.domain.order.enums.OrderStatus;
import com.cafe.domain.order.repository.OrderItemRepository;
import com.cafe.domain.order.repository.projection.PopularMenuProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderStatisticsReader {
    // 인기 메뉴 V1처럼 주문 통계성 조회를 담당한다.
    private final OrderItemRepository orderItemRepository;

    public List<PopularMenuProjection> findPopularMenus(LocalDateTime orderedFrom) {
        // RDB 원본 데이터 기준으로 최근 7일 PAID 주문만 집계한다.
        return orderItemRepository.findPopularMenus(
                OrderStatus.PAID,
                orderedFrom
        );
    }
}

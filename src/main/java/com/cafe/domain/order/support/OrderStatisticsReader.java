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
    private final OrderItemRepository orderItemRepository;

    public List<PopularMenuProjection> findPopularMenus(LocalDateTime orderedFrom) {
        return orderItemRepository.findPopularMenus(
                OrderStatus.PAID,
                orderedFrom
        );
    }
}

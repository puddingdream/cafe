package com.cafe.domain.order.repository;

import com.cafe.domain.order.entity.OrderItem;
import com.cafe.domain.order.enums.OrderStatus;
import com.cafe.domain.order.repository.projection.PopularMenuProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // 주문 단건 조회에서 상세 항목을 저장 순서대로 가져온다.
    List<OrderItem> findAllByOrderIdOrderByIdAsc(Long orderId);

    // 주문 목록 조회 시 여러 주문의 상세를 한 번에 가져와 N+1을 피한다.
    List<OrderItem> findAllByOrderIdInOrderByOrderIdAscIdAsc(Collection<Long> orderIds);

    @Query("""
            select new com.cafe.domain.order.repository.projection.PopularMenuProjection(
                menu,
                sum(orderItem.quantity)
            )
            from OrderItem orderItem
            join Order coffeeOrder on coffeeOrder.id = orderItem.orderId
            join Menu menu on menu.id = orderItem.menuId
            where coffeeOrder.status = :status
              and coffeeOrder.orderedAt >= :orderedFrom
            group by menu
            order by sum(orderItem.quantity) desc, menu.id asc
            limit 3
            """)
    // V1 인기 메뉴 조회: RDB에서 최근 7일 PAID 주문의 메뉴별 수량을 집계한다.
    List<PopularMenuProjection> findPopularMenus(
            @Param("status") OrderStatus status,
            @Param("orderedFrom") LocalDateTime orderedFrom
    );
}

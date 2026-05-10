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
    List<OrderItem> findAllByOrderIdOrderByIdAsc(Long orderId);

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
    List<PopularMenuProjection> findPopularMenus(
            @Param("status") OrderStatus status,
            @Param("orderedFrom") LocalDateTime orderedFrom
    );
}

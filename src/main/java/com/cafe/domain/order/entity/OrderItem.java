package com.cafe.domain.order.entity;

import com.cafe.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Table(
        name = "order_items",
        indexes = {
                @Index(name = "idx_order_items_order_id", columnList = "order_id"),
                @Index(name = "idx_order_items_menu_id", columnList = "menu_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE order_items SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class OrderItem extends BaseEntity {
    // 주문 당시의 메뉴명/가격을 스냅샷으로 저장하는 주문 상세 엔티티다.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Column(nullable = false)
    private String menuName;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private long menuPrice;

    @Column(nullable = false)
    private long totalPrice;

    @Builder
    private OrderItem(Long orderId, Long menuId, String menuName, int quantity, long menuPrice) {
        // 메뉴 가격 변경 이후에도 과거 주문 금액이 바뀌지 않도록 주문 시점 값을 저장한다.
        this.orderId = orderId;
        this.menuId = menuId;
        this.menuName = menuName;
        this.quantity = quantity;
        this.menuPrice = menuPrice;
        this.totalPrice = menuPrice * quantity;
    }
}

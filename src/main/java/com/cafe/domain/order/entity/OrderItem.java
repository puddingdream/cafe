package com.cafe.domain.order.entity;

import com.cafe.common.entity.BaseEntity;
import com.cafe.domain.menu.entity.Menu;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order coffeeOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", insertable = false, updatable = false)
    private Menu menu;

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
        this.orderId = orderId;
        this.menuId = menuId;
        this.menuName = menuName;
        this.quantity = quantity;
        this.menuPrice = menuPrice;
        this.totalPrice = menuPrice * quantity;
    }
}

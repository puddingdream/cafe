package com.cafe.domain.order.entity;

import com.cafe.common.entity.BaseEntity;
import com.cafe.domain.order.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE orders SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private long totalAmount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    @Builder
    private Order(String orderNumber, Long memberId, long totalAmount) {
        this.orderNumber = orderNumber;
        this.memberId = memberId;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PAID;
        this.orderedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = OrderStatus.CANCELED;
    }

    public boolean isPaid() {
        return this.status == OrderStatus.PAID;
    }
}

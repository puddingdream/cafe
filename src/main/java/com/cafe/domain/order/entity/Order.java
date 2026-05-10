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
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_member_ordered_at", columnList = "member_id, ordered_at"),
                @Index(name = "idx_orders_status_ordered_at", columnList = "status, ordered_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE orders SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Order extends BaseEntity {
    // 주문 마스터 엔티티다. 포인트 결제 완료 후 PAID 상태로 생성된다.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private long totalAmount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "ordered_at", nullable = false)
    private LocalDateTime orderedAt;

    @Builder
    private Order(String orderNumber, Long memberId, long totalAmount) {
        // 주문 생성 시점에 주문번호와 주문 시각을 확정한다.
        this.orderNumber = orderNumber;
        this.memberId = memberId;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PAID;
        this.orderedAt = LocalDateTime.now();
    }

    public void cancel() {
        // 주문 취소는 삭제가 아니라 상태 변경으로 남긴다.
        this.status = OrderStatus.CANCELED;
    }

    public boolean isPaid() {
        // 취소 가능 여부와 인기 메뉴 집계 기준을 판단할 때 사용한다.
        return this.status == OrderStatus.PAID;
    }
}

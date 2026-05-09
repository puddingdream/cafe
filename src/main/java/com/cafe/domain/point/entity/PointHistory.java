package com.cafe.domain.point.entity;

import com.cafe.common.entity.BaseEntity;
import com.cafe.domain.point.enums.PointHistoryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "point_histories",
        indexes = {
                @Index(name = "idx_point_histories_member_id", columnList = "member_id"),
                @Index(name = "idx_point_histories_wallet_id", columnList = "point_wallet_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "point_wallet_id", nullable = false)
    private Long pointWalletId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PointHistoryType type;

    @Column(nullable = false)
    private long point;

    @Column(nullable = false)
    private long afterPoint;

    @Builder
    private PointHistory(Long memberId, Long pointWalletId, PointHistoryType type, long point, long afterPoint) {
        this.memberId = memberId;
        this.pointWalletId = pointWalletId;
        this.type = type;
        this.point = point;
        this.afterPoint = afterPoint;
    }

    public static PointHistory charge(Long memberId, Long pointWalletId, long chargePoint, long afterPoint) {
        return PointHistory.builder()
                .memberId(memberId)
                .pointWalletId(pointWalletId)
                .type(PointHistoryType.CHARGE)
                .point(chargePoint)
                .afterPoint(afterPoint)
                .build();
    }

    public static PointHistory use(Long memberId, Long pointWalletId, long usedPoint, long afterPoint) {
        return PointHistory.builder()
                .memberId(memberId)
                .pointWalletId(pointWalletId)
                .type(PointHistoryType.USE)
                .point(usedPoint)
                .afterPoint(afterPoint)
                .build();
    }

    public static PointHistory refund(Long memberId, Long pointWalletId, long refundPoint, long afterPoint) {
        return PointHistory.builder()
                .memberId(memberId)
                .pointWalletId(pointWalletId)
                .type(PointHistoryType.REFUND)
                .point(refundPoint)
                .afterPoint(afterPoint)
                .build();
    }
}

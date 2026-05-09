package com.cafe.domain.point.entity;

import com.cafe.common.entity.BaseEntity;
import com.cafe.common.error.PointErrorCode;
import com.cafe.common.error.PointException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "point_wallets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE point_wallets SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class PointWallet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @Column(nullable = false)
    private long point;

    @Version
    private Long version;

    @Builder
    private PointWallet(Long memberId) {
        this.memberId = memberId;
        this.point = 0L;
    }

    public long charge(long chargePoint) {
        validatePositivePoint(chargePoint);
        this.point += chargePoint;
        return this.point;
    }

    public long refund(long refundPoint) {
        validatePositivePoint(refundPoint);
        this.point += refundPoint;
        return this.point;
    }

    public long use(long usedPoint) {
        validatePositivePoint(usedPoint);
        if (this.point < usedPoint) {
            throw new PointException(PointErrorCode.INSUFFICIENT_POINT);
        }
        this.point -= usedPoint;
        return this.point;
    }

    private void validatePositivePoint(long point) {
        if (point <= 0) {
            throw new PointException(PointErrorCode.INVALID_CHARGE_POINT);
        }
    }
}

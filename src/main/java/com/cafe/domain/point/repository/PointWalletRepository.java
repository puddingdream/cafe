package com.cafe.domain.point.repository;

import com.cafe.domain.point.entity.PointWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface PointWalletRepository extends JpaRepository<PointWallet, Long> {
    // 일반 지갑 조회에 사용한다.
    Optional<PointWallet> findByMemberId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    // 충전/사용/환불처럼 잔액 변경이 필요한 경우 지갑 row를 잠근다.
    Optional<PointWallet> findWithLockByMemberId(Long memberId);
}

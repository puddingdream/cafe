package com.cafe.domain.point.repository;

import com.cafe.domain.point.entity.PointWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface PointWalletRepository extends JpaRepository<PointWallet, Long> {
    Optional<PointWallet> findByMemberId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PointWallet> findWithLockByMemberId(Long memberId);
}

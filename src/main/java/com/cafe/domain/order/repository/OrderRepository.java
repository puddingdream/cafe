package com.cafe.domain.order.repository;

import com.cafe.domain.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findWithLockByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    Slice<Order> findAllByMemberIdOrderByOrderedAtDesc(Long memberId, Pageable pageable);
}

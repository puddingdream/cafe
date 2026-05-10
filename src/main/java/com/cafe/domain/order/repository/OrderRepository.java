package com.cafe.domain.order.repository;

import com.cafe.domain.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // 주문번호는 사용자에게 노출되는 단건 조회 식별자다.
    Optional<Order> findByOrderNumber(String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    // 취소 처리 중 같은 주문을 동시에 변경하지 못하도록 잠근다.
    Optional<Order> findWithLockByOrderNumber(String orderNumber);

    // 주문번호 충돌 방지를 위해 생성 직후 중복 여부를 확인한다.
    boolean existsByOrderNumber(String orderNumber);

    // 내 주문 목록을 최신 주문 순서로 Slice 조회한다.
    Slice<Order> findAllByMemberIdOrderByOrderedAtDesc(Long memberId, Pageable pageable);
}

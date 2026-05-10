package com.cafe.domain.point.repository;

import com.cafe.domain.point.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    // 현재는 저장 중심으로 사용하며, 향후 회원별 이력 조회 API로 확장할 수 있다.
}

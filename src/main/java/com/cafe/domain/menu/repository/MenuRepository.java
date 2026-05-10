package com.cafe.domain.menu.repository;

import com.cafe.domain.menu.entity.Menu;
import com.cafe.domain.menu.enums.MenuCategory;
import com.cafe.domain.menu.enums.MenuStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    // 메뉴명 중복 정책을 추가할 때 사용할 수 있는 조회다.
    boolean existsByName(String name);

    // 카테고리 없이 전체 메뉴를 최신 생성순으로 조회한다.
    List<Menu> findAllByOrderByCreatedAtDesc();

    // 카테고리 탭 화면에서 사용할 메뉴 조회다.
    List<Menu> findAllByCategoryOrderByCreatedAtDesc(MenuCategory category);

    // 판매 상태 기준 조회가 필요할 때 사용한다.
    List<Menu> findAllByStatusOrderByCreatedAtDesc(MenuStatus status);
}

package com.cafe.domain.menu.repository;

import com.cafe.domain.menu.entity.Menu;
import com.cafe.domain.menu.enums.MenuCategory;
import com.cafe.domain.menu.enums.MenuStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    boolean existsByName(String name);

    List<Menu> findAllByOrderByCreatedAtDesc();

    List<Menu> findAllByCategoryOrderByCreatedAtDesc(MenuCategory category);

    List<Menu> findAllByStatusOrderByCreatedAtDesc(MenuStatus status);
}

package com.cafe.domain.menu.support;

import com.cafe.common.error.MenuErrorCode;
import com.cafe.common.error.MenuException;
import com.cafe.domain.menu.entity.Menu;
import com.cafe.domain.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuReader {
    // 주문 도메인이 메뉴 저장소를 직접 알지 않도록 메뉴 조회를 모아둔다.

    private final MenuRepository menuRepository;

    public Menu findById(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new MenuException(MenuErrorCode.MENU_NOT_FOUND));
    }

    public List<Menu> findAllByIds(Collection<Long> menuIds) {
        return menuRepository.findAllById(menuIds);
    }
}

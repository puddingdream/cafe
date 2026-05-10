package com.cafe.domain.menu.enums;

import com.cafe.common.error.MenuErrorCode;
import com.cafe.common.error.MenuException;
import lombok.Getter;

@Getter
public enum MenuCategory {
    // 메뉴 카테고리는 현재 운영자가 동적으로 관리하지 않는 고정 분류로 둔다.
    COFFEE("커피"),
    LATTE("라떼"),
    TEA("티"),
    ADE("에이드"),
    SMOOTHIE("스무디"),
    DECAFFEINATED("디카페인"),
    DESSERT("디저트");

    private final String label;

    MenuCategory(String label) {
        this.label = label;
    }

    public static MenuCategory from(String value) {
        // API에서는 enum 이름과 한글 라벨을 모두 받을 수 있게 파싱한다.
        for (MenuCategory category : values()) {
            if (category.name().equalsIgnoreCase(value)
                    || category.label.equals(value)) {
                return category;
            }
        }

        throw new MenuException(MenuErrorCode.INVALID_MENU_CATEGORY);
    }
}

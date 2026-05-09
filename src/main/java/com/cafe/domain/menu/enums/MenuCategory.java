package com.cafe.domain.menu.enums;

import com.cafe.common.error.MenuErrorCode;
import com.cafe.common.error.MenuException;
import lombok.Getter;

@Getter
public enum MenuCategory {
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
        for (MenuCategory category : values()) {
            if (category.name().equalsIgnoreCase(value)
                    || category.label.equals(value)) {
                return category;
            }
        }

        throw new MenuException(MenuErrorCode.INVALID_MENU_CATEGORY);
    }
}

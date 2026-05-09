package com.cafe.domain.menu.entity;

import com.cafe.common.entity.BaseEntity;
import com.cafe.common.error.MenuErrorCode;
import com.cafe.common.error.MenuException;
import com.cafe.domain.menu.enums.MenuCategory;
import com.cafe.domain.menu.enums.MenuStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "menus")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE menus SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Menu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    private String imageUrl;

    private String imageKey;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MenuStatus status;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MenuCategory category;

    @Builder
    private Menu(String name, String description, String imageUrl, String imageKey, int price, MenuCategory category) {
        validatePrice(price);
        validateCategory(category);
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.imageKey = imageKey;
        this.price = price;
        this.status = MenuStatus.ACTIVE;
        this.category = category;
    }

    public void updateImage(String imageUrl, String imageKey) {
        this.imageUrl = imageUrl;
        this.imageKey = imageKey;
    }

    public void updateInfo(String name, String description, int price, MenuCategory category) {
        validatePrice(price);
        validateCategory(category);
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
    }

    public void toggleStatus() {
        this.status = this.status == MenuStatus.ACTIVE ? MenuStatus.INACTIVE : MenuStatus.ACTIVE;
    }

    public void changeStatus(MenuStatus status) {
        if (status == null) {
            throw new MenuException(MenuErrorCode.INVALID_MENU_STATUS);
        }
        this.status = status;
    }

    private void validatePrice(int price) {
        if (price <= 0) {
            throw new MenuException(MenuErrorCode.INVALID_MENU_PRICE);
        }
    }

    private void validateCategory(MenuCategory category) {
        if (category == null) {
            throw new MenuException(MenuErrorCode.INVALID_MENU_CATEGORY);
        }
    }
}

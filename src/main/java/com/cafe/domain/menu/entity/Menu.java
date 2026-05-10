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
    // 카페에서 판매하는 메뉴 정보다. 재고 수량 대신 판매 상태(ACTIVE/INACTIVE)로 주문 가능 여부를 관리한다.

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
        // 생성 시점부터 가격과 카테고리의 기본 도메인 규칙을 강제한다.
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
        // DB에는 이미지 바이너리 대신 외부 저장소 URL과 key만 저장한다.
        this.imageUrl = imageUrl;
        this.imageKey = imageKey;
    }

    public void updateInfo(String name, String description, int price, MenuCategory category) {
        // 이미지 외의 메뉴 기본 정보를 갱신한다.
        validatePrice(price);
        validateCategory(category);
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
    }

    public void toggleStatus() {
        // 관리자 화면에서 판매중/판매중지를 토글할 때 사용한다.
        this.status = this.status == MenuStatus.ACTIVE ? MenuStatus.INACTIVE : MenuStatus.ACTIVE;
    }

    public void changeStatus(MenuStatus status) {
        // 더미 데이터나 명시적 상태 변경이 필요할 때 사용하는 메서드다.
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

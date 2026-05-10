package com.cafe.domain.menu.service;

import com.cafe.common.error.MenuErrorCode;
import com.cafe.common.error.MenuException;
import com.cafe.domain.member.entity.Member;
import com.cafe.domain.member.enums.MemberRole;
import com.cafe.domain.member.support.MemberReader;
import com.cafe.domain.menu.dto.MenuCreateRequest;
import com.cafe.domain.menu.dto.MenuCreateResponse;
import com.cafe.domain.menu.dto.MenuGetResponse;
import com.cafe.domain.menu.dto.MenuUpdateRequest;
import com.cafe.domain.menu.dto.PopularMenuResponse;
import com.cafe.domain.menu.entity.Menu;
import com.cafe.domain.menu.enums.MenuCategory;
import com.cafe.domain.menu.repository.MenuRepository;
import com.cafe.domain.menu.support.MenuImageService;
import com.cafe.domain.order.support.OrderStatisticsReader;
import com.cafe.infrastructure.redis.CacheNames;
import com.cafe.infrastructure.security.dto.LoginUserInfoDto;
import com.cafe.infrastructure.storage.UploadedObject;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final MemberReader memberReader;
    private final MenuImageService menuImageService;
    private final MenuTransactionService menuTransactionService;
    private final OrderStatisticsReader orderStatisticsReader;

    @Transactional(readOnly = true)
    public List<MenuGetResponse> getMenus(String category) {
        List<Menu> menus = category == null || category.isBlank()
                ? menuRepository.findAllByOrderByCreatedAtDesc()
                : menuRepository.findAllByCategoryOrderByCreatedAtDesc(MenuCategory.from(category));

        return menus.stream()
                .map(MenuGetResponse::from)
                .toList();
    }

    @Cacheable(cacheNames = CacheNames.POPULAR_MENUS, key = "'top3:7d'")
    @Transactional(readOnly = true)
    public List<PopularMenuResponse> getPopularMenus() {
        LocalDateTime orderedFrom = LocalDateTime.now().minusDays(7);

        return orderStatisticsReader.findPopularMenus(orderedFrom)
                .stream()
                .map(popularMenu -> PopularMenuResponse.from(
                        popularMenu.menu(),
                        popularMenu.orderCount()
                ))
                .toList();
    }

    public MenuCreateResponse createMenu(MenuCreateRequest request, LoginUserInfoDto loginUser) {
        validateAdmin(loginUser.id());
        validatePrice(request.price());

        MenuCategory category = MenuCategory.from(request.category());
        UploadedObject uploadedImage = menuImageService.uploadRequired(request.imageFile());

        try {
            return menuTransactionService.createMenu(request, category, uploadedImage);
        } catch (RuntimeException exception) {
            menuImageService.deleteQuietly(uploadedImage.key());
            throw exception;
        }
    }

    public MenuGetResponse updateMenu(Long menuId, MenuUpdateRequest request, LoginUserInfoDto loginUser) {
        validateAdmin(loginUser.id());
        validatePrice(request.price());

        MenuCategory category = MenuCategory.from(request.category());
        UploadedObject uploadedImage = menuImageService.uploadOptional(request.imageFile());

        try {
            MenuTransactionService.UpdateMenuResult result = menuTransactionService.updateMenu(
                    menuId,
                    request,
                    category,
                    uploadedImage
            );
            if (uploadedImage != null) {
                menuImageService.deleteQuietly(result.previousImageKey());
            }

            return result.response();
        } catch (RuntimeException exception) {
            if (uploadedImage != null) {
                menuImageService.deleteQuietly(uploadedImage.key());
            }
            throw exception;
        }
    }

    public MenuGetResponse toggleMenuStatus(Long menuId, LoginUserInfoDto loginUser) {
        validateAdmin(loginUser.id());
        return menuTransactionService.toggleMenuStatus(menuId);
    }

    public void deleteMenu(Long menuId, LoginUserInfoDto loginUser) {
        validateAdmin(loginUser.id());

        MenuTransactionService.DeleteMenuResult result = menuTransactionService.deleteMenu(menuId);
        menuImageService.deleteQuietly(result.imageKey());
    }

    private void validateAdmin(Long memberId) {
        Member member = memberReader.findById(memberId);
        if (member.getRole() != MemberRole.ADMIN) {
            throw new MenuException(MenuErrorCode.FORBIDDEN_MENU_MANAGEMENT);
        }
    }

    private void validatePrice(int price) {
        if (price <= 0) {
            throw new MenuException(MenuErrorCode.INVALID_MENU_PRICE);
        }
    }
}

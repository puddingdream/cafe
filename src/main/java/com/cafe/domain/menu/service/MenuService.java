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
import com.cafe.domain.menu.entity.Menu;
import com.cafe.domain.menu.enums.MenuCategory;
import com.cafe.domain.menu.repository.MenuRepository;
import com.cafe.domain.menu.support.MenuImageService;
import com.cafe.infrastructure.security.dto.LoginUserInfoDto;
import com.cafe.infrastructure.storage.UploadedObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;
    private final MemberReader memberReader;
    private final MenuImageService menuImageService;

    public List<MenuGetResponse> getMenus(String category) {
        List<Menu> menus = category == null || category.isBlank()
                ? menuRepository.findAllByOrderByCreatedAtDesc()
                : menuRepository.findAllByCategoryOrderByCreatedAtDesc(MenuCategory.from(category));

        return menus.stream()
                .map(MenuGetResponse::from)
                .toList();
    }

    @Transactional
    public MenuCreateResponse createMenu(MenuCreateRequest request, LoginUserInfoDto loginUser) {
        validateAdmin(loginUser.id());
        validatePrice(request.price());

        MenuCategory category = MenuCategory.from(request.category());
        UploadedObject uploadedImage = menuImageService.uploadRequired(request.imageFile());
        registerRollbackImageCleanup(uploadedImage);

        Menu menu = Menu.builder()
                .name(request.name())
                .price(request.price())
                .description(request.description())
                .imageUrl(uploadedImage.url())
                .imageKey(uploadedImage.key())
                .category(category)
                .build();

        Menu savedMenu = menuRepository.save(menu);

        return MenuCreateResponse.from(savedMenu);
    }

    @Transactional
    public MenuGetResponse updateMenu(Long menuId, MenuUpdateRequest request, LoginUserInfoDto loginUser) {
        validateAdmin(loginUser.id());
        validatePrice(request.price());

        Menu menu = findMenu(menuId);
        MenuCategory category = MenuCategory.from(request.category());
        UploadedObject uploadedImage = menuImageService.uploadOptional(request.imageFile());

        if (uploadedImage != null) {
            registerRollbackImageCleanup(uploadedImage);
            String previousImageKey = menu.getImageKey();
            menu.updateImage(uploadedImage.url(), uploadedImage.key());
            registerAfterCommitImageDelete(previousImageKey);
        }

        menu.updateInfo(request.name(), request.description(), request.price(), category);
        return MenuGetResponse.from(menu);
    }

    @Transactional
    public MenuGetResponse toggleMenuStatus(Long menuId, LoginUserInfoDto loginUser) {
        validateAdmin(loginUser.id());

        Menu menu = findMenu(menuId);
        menu.toggleStatus();
        return MenuGetResponse.from(menu);
    }

    @Transactional
    public void deleteMenu(Long menuId, LoginUserInfoDto loginUser) {
        validateAdmin(loginUser.id());

        Menu menu = findMenu(menuId);
        String imageKey = menu.getImageKey();
        menuRepository.delete(menu);
        registerAfterCommitImageDelete(imageKey);
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

    private Menu findMenu(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new MenuException(MenuErrorCode.MENU_NOT_FOUND));
    }

    private void registerRollbackImageCleanup(UploadedObject uploadedImage) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    menuImageService.deleteQuietly(uploadedImage.key());
                }
            }
        });
    }

    private void registerAfterCommitImageDelete(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            menuImageService.deleteQuietly(imageKey);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                menuImageService.deleteQuietly(imageKey);
            }
        });
    }
}

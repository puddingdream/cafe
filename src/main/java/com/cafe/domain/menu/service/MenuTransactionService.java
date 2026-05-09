package com.cafe.domain.menu.service;

import com.cafe.common.error.MenuErrorCode;
import com.cafe.common.error.MenuException;
import com.cafe.domain.menu.dto.MenuCreateRequest;
import com.cafe.domain.menu.dto.MenuCreateResponse;
import com.cafe.domain.menu.dto.MenuGetResponse;
import com.cafe.domain.menu.dto.MenuUpdateRequest;
import com.cafe.domain.menu.entity.Menu;
import com.cafe.domain.menu.enums.MenuCategory;
import com.cafe.domain.menu.repository.MenuRepository;
import com.cafe.infrastructure.storage.UploadedObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MenuTransactionService {
    private final MenuRepository menuRepository;

    @Transactional
    public MenuCreateResponse createMenu(
            MenuCreateRequest request,
            MenuCategory category,
            UploadedObject uploadedImage
    ) {
        Menu menu = Menu.builder()
                .name(request.name())
                .price(request.price())
                .description(request.description())
                .imageUrl(uploadedImage.url())
                .imageKey(uploadedImage.key())
                .category(category)
                .build();

        return MenuCreateResponse.from(menuRepository.save(menu));
    }

    @Transactional
    public UpdateMenuResult updateMenu(
            Long menuId,
            MenuUpdateRequest request,
            MenuCategory category,
            UploadedObject uploadedImage
    ) {
        Menu menu = findMenu(menuId);
        String previousImageKey = null;

        if (uploadedImage != null) {
            previousImageKey = menu.getImageKey();
            menu.updateImage(uploadedImage.url(), uploadedImage.key());
        }

        menu.updateInfo(request.name(), request.description(), request.price(), category);
        return new UpdateMenuResult(MenuGetResponse.from(menu), previousImageKey);
    }

    @Transactional
    public MenuGetResponse toggleMenuStatus(Long menuId) {
        Menu menu = findMenu(menuId);
        menu.toggleStatus();
        return MenuGetResponse.from(menu);
    }

    @Transactional
    public DeleteMenuResult deleteMenu(Long menuId) {
        Menu menu = findMenu(menuId);
        String imageKey = menu.getImageKey();
        menuRepository.delete(menu);
        return new DeleteMenuResult(imageKey);
    }

    private Menu findMenu(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new MenuException(MenuErrorCode.MENU_NOT_FOUND));
    }

    public record UpdateMenuResult(
            MenuGetResponse response,
            String previousImageKey
    ) {
    }

    public record DeleteMenuResult(
            String imageKey
    ) {
    }
}

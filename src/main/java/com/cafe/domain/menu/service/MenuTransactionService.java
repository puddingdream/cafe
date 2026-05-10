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
    // 메뉴 DB 변경만 담당한다. 파일 업로드/삭제와 트랜잭션 경계를 분리하기 위한 서비스다.
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
            // 새 이미지가 정상 업로드된 경우에만 DB의 이미지 URL/key를 교체한다.
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
        // soft delete 후 외부 저장소 이미지도 삭제할 수 있게 key를 호출자에게 돌려준다.
        String imageKey = menu.getImageKey();
        menuRepository.delete(menu);
        return new DeleteMenuResult(imageKey);
    }

    private Menu findMenu(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new MenuException(MenuErrorCode.MENU_NOT_FOUND));
    }

    public record UpdateMenuResult(
            // DB 반영 결과와 삭제해야 할 이전 이미지 key를 함께 반환한다.
            MenuGetResponse response,
            String previousImageKey
    ) {
    }

    public record DeleteMenuResult(
            // DB 삭제 후 외부 저장소에서 제거할 이미지 key다.
            String imageKey
    ) {
    }
}

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
import com.cafe.domain.menu.support.PopularMenuRankingItem;
import com.cafe.domain.menu.support.PopularMenuRankingService;
import com.cafe.domain.order.support.OrderStatisticsReader;
import com.cafe.infrastructure.redis.CacheNames;
import com.cafe.infrastructure.security.dto.LoginUserInfoDto;
import com.cafe.infrastructure.storage.UploadedObject;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {
    // 메뉴 조회/관리의 파사드 역할을 한다. 이미지 업로드와 DB 트랜잭션은 하위 서비스로 분리한다.

    private final MenuRepository menuRepository;
    private final MemberReader memberReader;
    private final MenuImageService menuImageService;
    private final MenuTransactionService menuTransactionService;
    private final OrderStatisticsReader orderStatisticsReader;
    private final PopularMenuRankingService popularMenuRankingService;

    @Cacheable(cacheNames = CacheNames.MENUS, key = "#p0 == null || #p0.isBlank() ? 'all' : 'category:' + #p0.trim().toUpperCase()")
    @Transactional(readOnly = true)
    public List<MenuGetResponse> getMenus(String category) {
        // 카테고리가 없으면 전체 메뉴, 있으면 해당 카테고리 메뉴를 조회한다. 판매중지 메뉴도 화면 표시를 위해 포함한다.
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
        // V1: RDB 원본 주문 데이터를 기준으로 최근 7일 인기 메뉴를 집계한다.
        LocalDateTime orderedFrom = LocalDateTime.now().minusDays(7);

        return orderStatisticsReader.findPopularMenus(orderedFrom)
                .stream()
                .map(popularMenu -> PopularMenuResponse.from(
                        popularMenu.menu(),
                        popularMenu.orderCount()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PopularMenuResponse> getPopularMenusV2() {
        // V2: Redis ZSET read model에서 랭킹을 읽고, 화면에 필요한 메뉴 상세는 DB에서 다시 조립한다.
        List<PopularMenuRankingItem> rankings = popularMenuRankingService.findTopMenus(3);
        List<Long> menuIds = rankings.stream()
                .map(PopularMenuRankingItem::menuId)
                .toList();
        Map<Long, Menu> menuById = menuRepository.findAllById(menuIds)
                .stream()
                .collect(Collectors.toMap(Menu::getId, Function.identity()));

        return rankings.stream()
                .filter(ranking -> menuById.containsKey(ranking.menuId()))
                .map(ranking -> PopularMenuResponse.from(
                        menuById.get(ranking.menuId()),
                        ranking.orderCount()
                ))
                .toList();
    }

    @CacheEvict(cacheNames = CacheNames.MENUS, allEntries = true)
    public MenuCreateResponse createMenu(MenuCreateRequest request, LoginUserInfoDto loginUser) {
        // 파일 업로드는 DB 트랜잭션 밖에서 먼저 수행하고, DB 저장 실패 시 업로드 파일을 보상 삭제한다.
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

    @CacheEvict(cacheNames = CacheNames.MENUS, allEntries = true)
    public MenuGetResponse updateMenu(Long menuId, MenuUpdateRequest request, LoginUserInfoDto loginUser) {
        // 새 이미지가 있으면 먼저 업로드하고, DB 반영 성공 후 기존 이미지를 best-effort로 삭제한다.
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

    @CacheEvict(cacheNames = CacheNames.MENUS, allEntries = true)
    public MenuGetResponse toggleMenuStatus(Long menuId, LoginUserInfoDto loginUser) {
        validateAdmin(loginUser.id());
        return menuTransactionService.toggleMenuStatus(menuId);
    }

    @CacheEvict(cacheNames = CacheNames.MENUS, allEntries = true)
    public void deleteMenu(Long menuId, LoginUserInfoDto loginUser) {
        validateAdmin(loginUser.id());

        MenuTransactionService.DeleteMenuResult result = menuTransactionService.deleteMenu(menuId);
        menuImageService.deleteQuietly(result.imageKey());
    }

    private void validateAdmin(Long memberId) {
        // 메뉴 생성/수정/삭제는 관리자만 가능하다.
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

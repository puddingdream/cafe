package com.cafe.domain.menu.controller;

import com.cafe.common.dto.ApiResponse;
import com.cafe.domain.menu.dto.MenuCreateRequest;
import com.cafe.domain.menu.dto.MenuCreateResponse;
import com.cafe.domain.menu.dto.MenuGetResponse;
import com.cafe.domain.menu.dto.MenuUpdateRequest;
import com.cafe.domain.menu.dto.PopularMenuResponse;
import com.cafe.domain.menu.service.MenuService;
import com.cafe.infrastructure.security.annotation.LoginUser;
import com.cafe.infrastructure.security.dto.LoginUserInfoDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MenuController {
    // 메뉴 공개 조회와 관리자 메뉴 관리 API를 제공한다.

    private final MenuService menuService;

    @PostMapping(
            value = "/admin/menus",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<MenuCreateResponse>> createMenu(
            @Valid @ModelAttribute MenuCreateRequest request,
            @LoginUser LoginUserInfoDto loginUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(menuService.createMenu(request, loginUser)));
    }

    @GetMapping("/menus")
    public ResponseEntity<ApiResponse<List<MenuGetResponse>>> getMenus(
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getMenus(category)));
    }

    @GetMapping("/menus/popular")
    public ResponseEntity<ApiResponse<List<PopularMenuResponse>>> getPopularMenus() {
        return ResponseEntity.ok(ApiResponse.success(menuService.getPopularMenus()));
    }

    @GetMapping("/menus/popular/v2")
    public ResponseEntity<ApiResponse<List<PopularMenuResponse>>> getPopularMenusV2() {
        return ResponseEntity.ok(ApiResponse.success(menuService.getPopularMenusV2()));
    }

    @PutMapping(
            value = "/admin/menus/{menuId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<MenuGetResponse>> updateMenu(
            @PathVariable Long menuId,
            @Valid @ModelAttribute MenuUpdateRequest request,
            @LoginUser LoginUserInfoDto loginUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(menuService.updateMenu(menuId, request, loginUser)));
    }

    @PatchMapping("/admin/menus/{menuId}/status/toggle")
    public ResponseEntity<ApiResponse<MenuGetResponse>> toggleMenuStatus(
            @PathVariable Long menuId,
            @LoginUser LoginUserInfoDto loginUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(menuService.toggleMenuStatus(menuId, loginUser)));
    }

    @DeleteMapping("/admin/menus/{menuId}")
    public ResponseEntity<ApiResponse<String>> deleteMenu(
            @PathVariable Long menuId,
            @LoginUser LoginUserInfoDto loginUser
    ) {
        menuService.deleteMenu(menuId, loginUser);
        return ResponseEntity.ok(ApiResponse.success("메뉴가 삭제되었습니다."));
    }
}


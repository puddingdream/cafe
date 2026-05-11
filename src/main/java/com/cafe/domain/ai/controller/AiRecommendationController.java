package com.cafe.domain.ai.controller;

import com.cafe.common.dto.ApiResponse;
import com.cafe.domain.ai.dto.CoffeeRecommendationRequest;
import com.cafe.domain.ai.dto.CoffeeRecommendationResponse;
import com.cafe.domain.ai.service.AiRecommendationService;
import com.cafe.infrastructure.security.annotation.LoginUser;
import com.cafe.infrastructure.security.dto.LoginUserInfoDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiRecommendationController {
    // 로그인 사용자의 취향 입력을 받아 현재 판매중 메뉴 중에서 OpenAI 추천을 생성한다.
    // AI 호출은 외부 과금 API이므로 공개 API로 두지 않고 JWT 인증이 끝난 사용자만 접근하게 한다.

    private final AiRecommendationService aiRecommendationService;

    @PostMapping("/coffee-recommendations")
    public ResponseEntity<ApiResponse<CoffeeRecommendationResponse>> recommendCoffee(
            @Valid @RequestBody CoffeeRecommendationRequest request,
            @LoginUser LoginUserInfoDto loginUser
    ) {
        // @LoginUser에는 JWT 필터가 SecurityContext에 넣어둔 memberId가 들어온다.
        // 서비스는 이 memberId를 프롬프트 추적 정보로만 사용하고, 추천 후보는 DB/Redis의 실제 메뉴에서 고른다.
        return ResponseEntity.ok(ApiResponse.success(aiRecommendationService.recommendCoffee(request, loginUser)));
    }
}

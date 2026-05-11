package com.cafe.domain.ai.dto;

import com.cafe.domain.menu.entity.Menu;

import java.time.LocalDateTime;
import java.util.List;

public record CoffeeRecommendationResponse(
        // RECOMMENDATION이면 정상 추천, OUT_OF_SCOPE면 카페 메뉴 추천 외 요청을 거절한 응답이다.
        String answerType,

        // 추천 요약 또는 범위 밖 요청 안내 메시지다.
        // AI가 만든 문장이지만, 실제 메뉴명/가격/카테고리는 아래 recommendations의 DB 값이 기준이다.
        String message,

        // 서버가 검증한 실제 판매중 메뉴 추천 목록이다.
        // AI가 반환한 menuId가 후보에 없으면 이 목록에 들어오지 못한다.
        List<RecommendedDrink> recommendations,

        // 추천 후보로 모델에 전달한 판매중 메뉴 개수다.
        int menuCount,

        // 응답 생성 시각이다.
        LocalDateTime recommendedAt
) {
    public static CoffeeRecommendationResponse of(String message, List<RecommendedDrink> recommendations, int menuCount) {
        return new CoffeeRecommendationResponse("RECOMMENDATION", message, recommendations, menuCount, LocalDateTime.now());
    }

    public static CoffeeRecommendationResponse outOfScope(int menuCount) {
        // 범위 밖 요청은 AI가 자세히 답변하지 않도록 고정 문구와 빈 추천 목록으로 응답한다.
        return new CoffeeRecommendationResponse(
                "OUT_OF_SCOPE",
                "카페 메뉴 추천과 메뉴 정보 안내만 도와드릴 수 있습니다.",
                List.of(),
                menuCount,
                LocalDateTime.now()
        );
    }

    public record RecommendedDrink(
            // 실제 DB 메뉴 ID다. AI 응답값을 그대로 쓰지 않고 Menu 엔티티에서 가져온다.
            Long menuId,

            // 실제 DB 메뉴명이다. 없는 메뉴 추천을 막기 위해 서버가 DB 값으로 조립한다.
            String name,

            // 실제 DB 메뉴 설명이다.
            String description,

            // 실제 DB 메뉴 이미지 URL이다.
            String imageUrl,

            // 실제 DB 메뉴 가격이다.
            int price,

            // 실제 DB 메뉴 카테고리 enum 이름이다.
            String category,

            // 실제 DB 메뉴 카테고리 한글 라벨이다.
            String categoryLabel,

            // AI가 생성한 추천 이유다. 사용자의 취향과 실제 메뉴를 연결하는 설명만 담당한다.
            String reason,

            // AI가 생성한 카페인 정보다. 정확한 DB 수치가 아니면 추정으로 표현한다.
            String caffeineInfo,

            // AI가 생성한 칼로리 정보다. 정확한 DB 수치가 아니면 추정으로 표현한다.
            String calorieInfo,

            // AI가 생성한 음용 팁이다.
            String servingTip
    ) {
        public static RecommendedDrink from(Menu menu, AiRecommendedDrink recommendation) {
            // 메뉴의 신뢰 가능한 정보는 DB에서, 취향 기반 설명은 AI 응답에서 조합한다.
            // 이렇게 해야 AI가 없는 메뉴명이나 잘못된 가격을 만들어도 최종 응답에 섞이지 않는다.
            return new RecommendedDrink(
                    menu.getId(),
                    menu.getName(),
                    menu.getDescription(),
                    menu.getImageUrl(),
                    menu.getPrice(),
                    menu.getCategory().name(),
                    menu.getCategory().getLabel(),
                    recommendation.reason(),
                    recommendation.caffeineInfo(),
                    recommendation.calorieInfo(),
                    recommendation.servingTip()
            );
        }
    }
}

package com.cafe.domain.ai.dto;

public record AiRecommendedDrink(
        // 반드시 프롬프트에 제공된 실제 판매중 메뉴 ID여야 한다.
        // AI가 없는 ID를 만들 수 있으므로 서버가 최종 검증한다.
        Long menuId,

        // 사용자의 취향과 메뉴 특성을 연결하는 추천 이유다.
        String reason,

        // 현재 DB에는 카페인 수치 컬럼이 없으므로 정확한 수치처럼 단정하지 않고 "추정"임을 포함해야 한다.
        String caffeineInfo,

        // 현재 DB에는 칼로리 컬럼이 없으므로 정확한 수치처럼 단정하지 않고 "추정"임을 포함해야 한다.
        String calorieInfo,

        // 마시는 방식, 온도, 같이 먹기 좋은 메뉴 등 짧은 팁이다.
        String servingTip
) {
}

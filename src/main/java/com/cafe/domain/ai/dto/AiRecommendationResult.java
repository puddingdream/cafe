package com.cafe.domain.ai.dto;

import java.util.List;

public record AiRecommendationResult(
        // AI가 반환해야 하는 최상위 응답 타입이다.
        // 서버는 RECOMMENDATION이면 menuId 검증을 진행하고, OUT_OF_SCOPE이면 추천 없이 안내 응답을 만든다.
        String answerType,

        // 사용자에게 보여줄 짧은 안내 또는 추천 요약이다. 이 값은 보조 문구이고, 메뉴 정보의 source of truth는 DB다.
        String message,

        // AI가 고른 후보 메뉴 ID와 추천 사유다.
        // 이 목록은 신뢰하지 않고, 서버가 후보 메뉴 목록에 포함된 실제 menuId인지 다시 검증한다.
        List<AiRecommendedDrink> recommendations
) {
}

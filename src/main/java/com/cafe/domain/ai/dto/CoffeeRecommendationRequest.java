package com.cafe.domain.ai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CoffeeRecommendationRequest(
        // 사용자가 원하는 맛, 상황, 기분을 자유롭게 적는 필수 입력이다.
        // 프롬프트 인젝션성 문장이 들어올 수 있으므로 길이를 제한하고, 시스템 프롬프트에서 카페 메뉴 범위 밖 요청을 거절한다.
        @NotBlank(message = "추천받고 싶은 취향이나 상황은 필수입니다.")
        @Size(max = 300, message = "취향 입력은 300자 이하로 입력해주세요.")
        String preference,

        // 예: HOT, ICE, 상관없음. 값 자체는 자유 텍스트지만 프롬프트에 보조 조건으로만 반영한다.
        @Size(max = 30, message = "온도 취향은 30자 이하로 입력해주세요.")
        String temperaturePreference,

        // 예: 카페인 가능, 디카페인 선호, 상관없음. 메뉴 후보를 강제로 삭제하지 않고 추천 판단 기준으로 전달한다.
        @Size(max = 50, message = "카페인 취향은 50자 이하로 입력해주세요.")
        String caffeinePreference,

        // 예산이 있으면 해당 가격 이하 메뉴를 우선 추천하도록 프롬프트에 반영한다.
        // 현재는 AI 추천 조건으로만 쓰며, 서버에서 가격 필터를 강제하지는 않는다.
        @Min(value = 0, message = "최대 예산은 0원 이상이어야 합니다.")
        Integer maxPrice
) {
}

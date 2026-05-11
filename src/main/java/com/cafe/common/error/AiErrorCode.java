package com.cafe.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCodeType {
    // OpenAI/Spring AI 추천 기능에서 사용하는 에러 코드다.
    AI_RECOMMENDATION_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "AI001", "AI 추천 기능이 설정되어 있지 않습니다."),
    NO_RECOMMENDABLE_MENU(HttpStatus.NOT_FOUND, "AI002", "추천 가능한 판매중 메뉴가 없습니다."),
    AI_RECOMMENDATION_FAILED(HttpStatus.BAD_GATEWAY, "AI003", "AI 추천 생성에 실패했습니다."),
    INVALID_AI_RECOMMENDATION(HttpStatus.BAD_GATEWAY, "AI004", "AI가 유효하지 않은 메뉴 추천을 반환했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

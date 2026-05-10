package com.cafe.common.constant;

public final class AuthConstants {
    // 인증/토큰 처리에서 반복 사용되는 문자열 상수 모음이다.
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String SIGNUP_SUCCESS_MESSAGE = "회원가입이 완료되었습니다.";
    public static final String LOGOUT_SUCCESS_MESSAGE = "로그아웃이 처리되었습니다.";

    private AuthConstants() {
    }
}

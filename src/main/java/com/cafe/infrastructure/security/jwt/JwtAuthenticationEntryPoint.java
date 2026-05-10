package com.cafe.infrastructure.security.jwt;

import com.cafe.common.dto.ApiResponse;
import com.cafe.common.dto.ErrorResponse;
import com.cafe.common.error.MemberErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    // Security filter 단계에서 발생한 미인증 오류는 GlobalExceptionHandler가 잡지 못하므로 직접 응답을 만든다.
    private final JsonMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        Object exception = request.getAttribute("exception");

        if (exception instanceof ExpiredJwtException) {
            setResponse(request, response, MemberErrorCode.EXPIRED_TOKEN);
            return;
        }

        if (exception != null) {
            setResponse(request, response, MemberErrorCode.INVALID_TOKEN);
            return;
        }

        setResponse(request, response, MemberErrorCode.UNAUTHORIZED_ACCESS);
    }

    private void setResponse(HttpServletRequest request, HttpServletResponse response, MemberErrorCode errorCode)
            throws IOException {
        // 인증 실패 응답도 ApiResponse 구조와 비슷하게 맞춰 클라이언트 처리를 단순하게 한다.
        log.warn("Authentication failed. code={}, path={}", errorCode.getCode(), request.getRequestURI());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(errorCode.getStatus().value());

        ApiResponse<Void> body = ApiResponse.fail(ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .error(errorCode.getStatus().name())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .path(request.getRequestURI())
                .build());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

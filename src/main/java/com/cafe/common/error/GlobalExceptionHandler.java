package com.cafe.common.error;

import com.cafe.common.dto.ApiResponse;
import com.cafe.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
// 애플리케이션 전역 예외를 공통 응답 형식으로 변환한다.
public class GlobalExceptionHandler {

    /**
     * 400: 잘못된 요청을 공통 에러 응답으로 반환
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("IllegalArgumentException : {}", e.getMessage());
        return ResponseEntity
                .badRequest() // 400 Bad Request
                .body(ApiResponse.fail(buildErrorResponse(ErrorCode.INVALID_INPUT_VALUE, e.getMessage(), request.getRequestURI())));
    }

    /**
     * 404: 리소스가 존재하지 않는 경우 공통 에러 응답으로 반환
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException e, HttpServletRequest request) {
        log.warn("IllegalStateException : {}", e.getMessage());
        ErrorCodeType errorCode = MemberErrorCode.MEMBER_NOT_FOUND;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(buildErrorResponse(errorCode, e.getMessage(), request.getRequestURI())));
    }

    /**
     * @Valid 어노테이션을 통한 입력값 검증 실패 시 발생하는 예외를 처리합니다.
     * 예: 비밀번호가 정규표현식에 맞지 않거나, 필수값이 누락된 경우
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        log.warn("MethodArgumentNotValidException : {}", e.getMessage());
        // 검증 실패 메시지 중 첫 번째 메시지를 가져옵니다.
        String errorMessage = e.getBindingResult().getFieldError().getDefaultMessage();
        return ResponseEntity
                .badRequest() // 400 Bad Request
                .body(ApiResponse.fail(buildErrorResponse(ErrorCode.INVALID_INPUT_VALUE, errorMessage, request.getRequestURI())));
    }

    /**
     * 400: QueryParam/pathVariable Bean Validation 실패(@Min/@Max) 시
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        log.warn("ConstraintViolationException : {}", e.getMessage());
        // 검증 실패 메시지 중 첫 번째 메시지를 가져옵니다.
        String errorMessage = e.getConstraintViolations()
                .stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse("요청 값이 유효하지 않음");
        return ResponseEntity
                .badRequest() // 400 Bad Request
                .body(ApiResponse.fail(buildErrorResponse(ErrorCode.INVALID_INPUT_VALUE, errorMessage, request.getRequestURI())));
    }

    /**
     * 4xx/5xx: 회원 도메인 커스텀 예외 처리
     */
    @ExceptionHandler(MemberException.class)
    public ResponseEntity<ApiResponse<Void>> handleMemberException(MemberException e, HttpServletRequest request) {
        return handleCustomException("MemberException", e.getErrorCode(), e, request);
    }

    /**
     * 4xx/5xx: 공통 비즈니스 예외 처리 (인증 실패, 로그인, 회원가입)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        return handleCustomException("BusinessException", e.getErrorCode(), e, request);
    }

    /**
     * 403: 권한이 없는 요청(인가 실패) 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("AccessDeniedException : {}", e.getMessage());
        ErrorCodeType errorCode = ErrorCode.ACCESS_DENIED;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(buildErrorResponse(errorCode, errorCode.getMessage(), request.getRequestURI())));
    }

    /**
     * 위에서 처리하지 못한 나머지 모든 예외(Exception)를 처리합니다.
     * 예상치 못한 서버 에러(NullPointerException 등)가 여기에 해당합니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e, HttpServletRequest request) {
        log.error("Exception : {}", e.getMessage());
        // 보안을 위해 내부 에러 메시지는 숨기고, "서버 내부 오류"라는 일반적인 메시지를 반환합니다.
        return ResponseEntity
                .internalServerError() // 500 Internal Server Error
                .body(ApiResponse.fail(buildErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "알 수 없는 에러가 발생했습니다.", request.getRequestURI())));
    }

    /**
     * ErrorResponse 객체를 생성하는 유틸리티 메서드입니다.
     */
    private ErrorResponse buildErrorResponse(ErrorCodeType errorCode, String message, String path) {
        // 컨트롤러 밖에서도 동일한 에러 응답 구조를 유지하기 위한 조립 메서드다.
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .error(errorCode.getStatus().name())
                .code(errorCode.getCode())
                .message(message)
                .path(path)
                .build();
    }

    /**
     * ErrorCode를 가진 커스텀 예외는 이 메서드로 공통 응답 형식을 맞춘다.
     */
    private ResponseEntity<ApiResponse<Void>> handleCustomException(
            String exceptionName,
            ErrorCodeType errorCode,
            RuntimeException e,
            HttpServletRequest request
    ) {
        log.warn("{} : {}", exceptionName, e.getMessage());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(buildErrorResponse(errorCode, e.getMessage(), request.getRequestURI())));
    }
}


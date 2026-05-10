package com.cafe.domain.auth.controller;

import com.cafe.common.constant.AuthConstants;
import com.cafe.common.dto.ApiResponse;
import com.cafe.domain.auth.dto.AuthTokens;
import com.cafe.domain.auth.dto.LoginRequest;
import com.cafe.domain.auth.dto.LoginResponse;
import com.cafe.domain.auth.dto.LogoutRequest;
import com.cafe.domain.auth.dto.ReissueTokenRequest;
import com.cafe.domain.auth.dto.SignUpRequest;
import com.cafe.domain.auth.dto.SignUpResponse;
import com.cafe.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    // 인증 관련 HTTP 요청을 받고 실제 로직은 AuthService로 위임한다.
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignUpResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(authService.signUp(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<AuthTokens>> reissue(@Valid @RequestBody ReissueTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.reissue(request.refreshToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(AuthConstants.LOGOUT_SUCCESS_MESSAGE));
    }
}

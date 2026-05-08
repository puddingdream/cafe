package com.cafe.domain.auth.service;

import com.cafe.common.constant.AuthConstants;
import com.cafe.common.error.MemberErrorCode;
import com.cafe.common.error.MemberException;
import com.cafe.domain.auth.dto.AuthTokens;
import com.cafe.domain.auth.dto.LoginRequest;
import com.cafe.domain.auth.dto.LoginResponse;
import com.cafe.domain.auth.dto.SignUpRequest;
import com.cafe.domain.auth.dto.SignUpResponse;
import com.cafe.domain.auth.entity.RefreshToken;
import com.cafe.domain.auth.repository.RefreshTokenRepository;
import com.cafe.domain.member.entity.Member;
import com.cafe.domain.member.service.MemberService;
import com.cafe.domain.member.support.MemberReader;
import com.cafe.infrastructure.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final MemberService memberService;
    private final MemberReader memberReader;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        Member member = memberService.createMember(request);
        return new SignUpResponse(member.getId(), member.getEmail(), AuthConstants.SIGNUP_SUCCESS_MESSAGE);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Member member = memberReader.findByEmail(request.email());
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new MemberException(MemberErrorCode.INVALID_CREDENTIALS);
        }

        AuthTokens tokens = generateTokens(member);
        refreshTokenRepository.findByMemberId(member.getId())
                .ifPresentOrElse(
                        refreshToken -> refreshToken.updateToken(tokens.refreshToken()),
                        () -> refreshTokenRepository.save(RefreshToken.builder()
                                .memberId(member.getId())
                                .token(tokens.refreshToken())
                                .build())
                );

        return new LoginResponse(
                tokens,
                new LoginResponse.MemberInfo(member.getId(), member.getEmail(), member.getName())
        );
    }

    @Transactional
    public AuthTokens reissue(String refreshToken) {
        jwtTokenProvider.validateToken(refreshToken);

        RefreshToken tokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new MemberException(MemberErrorCode.UNAUTHORIZED_ACCESS));
        Member member = memberReader.findById(tokenEntity.getMemberId());

        AuthTokens tokens = generateTokens(member);
        tokenEntity.updateToken(tokens.refreshToken());
        return tokens;
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }

    private AuthTokens generateTokens(Member member) {
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().getKey());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());
        return new AuthTokens(accessToken, refreshToken);
    }
}

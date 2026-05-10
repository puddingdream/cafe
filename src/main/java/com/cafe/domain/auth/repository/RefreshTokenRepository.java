package com.cafe.domain.auth.repository;

import com.cafe.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    // 재발급/로그아웃 요청으로 들어온 token이 서버 저장소에 있는지 확인한다.
    Optional<RefreshToken> findByToken(String token);

    // 로그인 시 회원에게 이미 발급된 refresh token이 있는지 확인한다.
    Optional<RefreshToken> findByMemberId(Long memberId);
}

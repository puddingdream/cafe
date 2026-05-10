package com.cafe.infrastructure.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    // JWT 생성/검증과 claim 추출을 담당한다.
    private static final String ISSUER = "CafeAuthServer";

    @Value("${jwt.secret-key}")
    private String secretKeyString;

    @Value("${jwt.access-token-validity-time}")
    private long accessTokenValidityInMilliseconds;

    @Value("${jwt.refresh-token-validity-time}")
    private long refreshTokenValidityInMilliseconds;

    private SecretKey key;

    @PostConstruct
    public void init() {
        // HMAC SHA 계열 서명에 충분한 길이의 Base64 secret인지 애플리케이션 시작 시 검증한다.
        byte[] keyBytes = Decoders.BASE64.decode(secretKeyString);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret-key는 최소 32바이트(256bit) 이상이어야 합니다.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Long memberId, String role) {
        return buildToken(memberId, role, accessTokenValidityInMilliseconds);
    }

    public String createRefreshToken(Long memberId) {
        return buildToken(memberId, null, refreshTokenValidityInMilliseconds);
    }

    public Long getMemberId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        parseClaims(token);
        return true;
    }

    private String buildToken(Long memberId, String role, long validityTimeInMilliseconds) {
        // subject에는 memberId를 넣고, access token일 때만 role claim을 추가한다.
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityTimeInMilliseconds);

        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(memberId))
                .issuer(ISSUER)
                .issuedAt(now)
                .expiration(validity)
                .id(UUID.randomUUID().toString())
                .signWith(key);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    private Claims parseClaims(String token) {
        // 서명, issuer, 만료 시간을 검증하고 payload를 반환한다.
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

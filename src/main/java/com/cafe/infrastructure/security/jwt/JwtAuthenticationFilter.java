package com.cafe.infrastructure.security.jwt;

import com.cafe.common.constant.AuthConstants;
import com.cafe.infrastructure.security.PublicPathPatterns;
import com.cafe.infrastructure.security.dto.LoginUserInfoDto;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // 매 요청마다 Authorization 헤더의 JWT를 검증하고 SecurityContext에 인증 정보를 넣는다.
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 공개 API는 JWT가 없어도 접근 가능하므로 필터 검사를 건너뛴다.
        String path = request.getRequestURI();
        return matches(PublicPathPatterns.ANY_METHOD, path)
                || ("POST".equalsIgnoreCase(request.getMethod()) && matches(PublicPathPatterns.PUBLIC_POST, path))
                || ("GET".equalsIgnoreCase(request.getMethod()) && matches(PublicPathPatterns.PUBLIC_GET, path));
    }
    private boolean matches(String[] patterns, String path) {
        // Spring AntPathMatcher로 /swagger-ui/** 같은 패턴 매칭을 처리한다.
        return Arrays.stream(patterns).anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);

        try {
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                // token에서 꺼낸 memberId를 principal로 넣어 @LoginUser에서 사용할 수 있게 한다.
                Long memberId = jwtTokenProvider.getMemberId(token);
                String role = jwtTokenProvider.getRole(token);

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        LoginUserInfoDto.builder().id(memberId).build(), // 인증사용자정보
                        null,                                            // 인증 수단/비밀값
                        Collections.singletonList(new SimpleGrantedAuthority(StringUtils.hasText(role) ? role : "ROLE_USER"))
                );                                                       // 권한 목록
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (ExpiredJwtException exception) {
            // 필터 내부 예외는 전역 예외 처리기를 거치지 않으므로 request attribute로 EntryPoint에 넘긴다.
            log.warn("Expired JWT token: {}", exception.getMessage());
            request.setAttribute("exception", exception);
        } catch (SecurityException | MalformedJwtException | SignatureException exception) {
            log.warn("Invalid JWT signature: {}", exception.getMessage());
            request.setAttribute("exception", exception);
        } catch (UnsupportedJwtException exception) {
            log.warn("Unsupported JWT token: {}", exception.getMessage());
            request.setAttribute("exception", exception);
        } catch (RuntimeException exception) {
            log.warn("JWT validation failed: {}", exception.getMessage());
            request.setAttribute("exception", exception);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // Authorization: Bearer {token} 형식이면 Bearer prefix를 제거하고 JWT 본문만 반환한다.
        String bearerToken = request.getHeader(AuthConstants.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(AuthConstants.BEARER_PREFIX)) {
            return bearerToken.substring(AuthConstants.BEARER_PREFIX.length());
        }
        return null;
    }
}

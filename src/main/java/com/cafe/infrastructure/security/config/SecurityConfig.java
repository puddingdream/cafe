package com.cafe.infrastructure.security.config;

import com.cafe.infrastructure.security.PublicPathPatterns;
import com.cafe.infrastructure.security.jwt.JwtAuthenticationEntryPoint;
import com.cafe.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.cafe.infrastructure.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    // JWT 기반 stateless API 보안 설정이다.
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // REST API 서버이므로 세션/폼로그인/HTTP Basic은 사용하지 않는다.
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 가능한 경로를 먼저 열어두고 나머지는 인증을 요구한다.
                        .requestMatchers(PublicPathPatterns.ANY_METHOD).permitAll()
                        .requestMatchers(HttpMethod.POST, PublicPathPatterns.PUBLIC_POST).permitAll()
                        .requestMatchers(HttpMethod.GET, PublicPathPatterns.PUBLIC_GET).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        // UsernamePasswordAuthenticationFilter보다 먼저 JWT를 읽어 SecurityContext를 채운다.
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}

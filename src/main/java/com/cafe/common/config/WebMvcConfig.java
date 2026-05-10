package com.cafe.common.config;

import com.cafe.infrastructure.security.resolver.LoginUserArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    // @LoginUser 컨트롤러 파라미터를 해석하는 커스텀 ArgumentResolver다.
    private final LoginUserArgumentResolver loginUserArgumentResolver;

    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
        // Spring MVC가 컨트롤러 호출 전에 @LoginUser 값을 주입할 수 있게 등록한다.
        resolvers.add(loginUserArgumentResolver);
    }
}

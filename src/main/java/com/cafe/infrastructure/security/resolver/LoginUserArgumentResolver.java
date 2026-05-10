package com.cafe.infrastructure.security.resolver;

import com.cafe.common.error.MemberErrorCode;
import com.cafe.common.error.MemberException;
import com.cafe.infrastructure.security.annotation.LoginUser;
import com.cafe.infrastructure.security.dto.LoginUserInfoDto;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        // @LoginUser LoginUserInfoDto 형태의 파라미터만 이 resolver가 처리한다.
        return parameter.hasParameterAnnotation(LoginUser.class)
                && LoginUserInfoDto.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            @NonNull MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            @NonNull NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory
    ) {
        // JwtAuthenticationFilter가 SecurityContext에 넣어둔 principal을 컨트롤러 파라미터로 반환한다.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || authentication.getPrincipal().equals("anonymousUser")
                || !(authentication.getPrincipal() instanceof LoginUserInfoDto)) {
            throw new MemberException(MemberErrorCode.UNAUTHORIZED_ACCESS);
        }
        return authentication.getPrincipal();
    }
}

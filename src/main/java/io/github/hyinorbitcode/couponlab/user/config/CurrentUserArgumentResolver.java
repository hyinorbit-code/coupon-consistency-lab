package io.github.hyinorbitcode.couponlab.user.config;

import io.github.hyinorbitcode.couponlab.user.CurrentUser;
import io.github.hyinorbitcode.couponlab.user.Role;
import io.github.hyinorbitcode.couponlab.user.security.JwtTokenIssuer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Optional;

/**
 * 컨트롤러 파라미터에 로그인 사용자를 넣는다.
 * {@code CurrentUser}는 로그인이 필요한 API에, {@code Optional<CurrentUser>}는 로그인하지 않아도 되는 API에 쓴다.
 */
@Component
class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == CurrentUser.class || isOptionalCurrentUser(parameter);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        Optional<CurrentUser> currentUser = currentUser();
        if (isOptionalCurrentUser(parameter)) {
            return currentUser;
        }
        return currentUser.orElseThrow(() -> new IllegalStateException("no authenticated user"));
    }

    private static Optional<CurrentUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        return Optional.of(new CurrentUser(
                Long.parseLong(jwt.getSubject()),
                Role.valueOf(jwt.getClaimAsString(JwtTokenIssuer.ROLE_CLAIM))));
    }

    private static boolean isOptionalCurrentUser(MethodParameter parameter) {
        if (parameter.getParameterType() != Optional.class) {
            return false;
        }
        return ResolvableType.forMethodParameter(parameter).getGeneric(0).resolve() == CurrentUser.class;
    }
}

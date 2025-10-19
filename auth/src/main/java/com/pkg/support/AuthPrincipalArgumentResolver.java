package com.pkg.support;

import com.pkg.authentication.token.MemberPrincipal;
import com.pkg.core.AuthenticationException;
import com.pkg.core.AuthenticationExceptionType;
import com.pkg.domain.member.Actor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class AuthPrincipalArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Authenticated.class)
                && parameter.getParameterType().equals(Actor.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        MemberPrincipal principal = (MemberPrincipal) webRequest.getAttribute(RequestAttributeKey.PRINCIPAL.name(), RequestAttributes.SCOPE_REQUEST);
        throwIfNoPrincipal(principal);
        return new Actor(
                principal.getMemberId(),
                principal.getRole()
        );
    }

    private static void throwIfNoPrincipal(MemberPrincipal principal) {
        if(principal == null) {
            throw new AuthenticationException(
                    "member principal is NULL",
                    AuthenticationExceptionType.UNKNOWN
            );
        }
    }
}

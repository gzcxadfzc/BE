package com.pkg.support;

import com.pkg.authentication.token.AccessToken;
import com.pkg.authentication.token.MemberPrincipal;
import com.pkg.authentication.core.AuthenticationException;
import com.pkg.authentication.core.Authenticator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final PathMatcher PATH_MATCHER = new AntPathMatcher();
    private final List<String> shouldNotFilterUriList;
    private final Authenticator<AccessToken, MemberPrincipal> authenticator;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public JwtAuthenticationFilter(List<String> shouldNotFilterUriList, Authenticator<AccessToken, MemberPrincipal> authenticator, HandlerExceptionResolver handlerExceptionResolver) {
        this.shouldNotFilterUriList = shouldNotFilterUriList;
        this.authenticator = authenticator;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        log.info("JwtAuthenticationFilter invoked: {}", request.getRequestURI());
        try {
            String bearerToken = parseBearerToken(request);
            MemberPrincipal memberPrincipal = authenticator.authenticate(new AccessToken(bearerToken));
            request.setAttribute(RequestAttributeKey.PRINCIPAL.name(), memberPrincipal);
            filterChain.doFilter(request, response);
        } catch (AuthenticationException e) {
            handlerExceptionResolver.resolveException(request, response, null,e);
        }
    }

    private String parseBearerToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw AuthenticationException.missingBearerTokenException("cannot find bearer token.");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return shouldNotFilterUriList.stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, requestURI));
    }
}

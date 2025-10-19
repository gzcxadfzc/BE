package com.pkg;

import com.auth0.jwt.algorithms.Algorithm;
import com.pkg.authentication.token.*;
import com.pkg.configs.RSAKeyFactory;
import com.pkg.core.AuthenticationException;
import com.pkg.core.Authenticator;
import com.pkg.domain.member.Actor;
import com.pkg.domain.member.Role;
import com.pkg.support.Authenticated;
import com.pkg.support.AuthPrincipalArgumentResolver;
import com.pkg.support.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

public class AuthTestConfig {

    @RestController
    public static class DummyController {

        @GetMapping("/public")
        public String test() {
            return "OK";
        }

        @GetMapping("/secure")
        public Actor secure(@Authenticated Actor currentUser) {
            return currentUser;
        }
    }

    @RestControllerAdvice
    public static class TestControllerAdvice {

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
            ErrorResponse errorResponse = new ErrorResponse(
                    e.getType().name(),
                    e.getMessage()
            );
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(errorResponse);
        }

        record ErrorResponse(String errorType, String message) {}
    }

    @TestConfiguration
    public static class TestConfig implements WebMvcConfigurer {

        @Bean
        public AuthPrincipalArgumentResolver authPrincipalArgumentResolver() {
            return new AuthPrincipalArgumentResolver();
        }

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(authPrincipalArgumentResolver());
        }

        @Bean
        Authenticator<AccessToken, MemberPrincipal> mockAuthenticator() {
            return credential -> {
                String value = credential.getValue();
                return switch (value) {
                    case VALID_MEMBER_JWT -> VALID_MEMBER_PRINCIPAL;
                    case VALID_ADMIN_JWT -> VALID_ADMIN_PRINCIPAL;
                    case VALID_GUEST_JWT -> VALID_GUEST_PRINCIPAL;
                    default -> throw AuthenticationException.invalidClaimException();
                };
            };
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(
                Authenticator<AccessToken, MemberPrincipal> tokenAuthenticator,
                @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
        ) {
            List<String> shouldNotFilterUriList = List.of("/public");
            return new JwtAuthenticationFilter(shouldNotFilterUriList, tokenAuthenticator, resolver);
        }

        @Bean
        RSAKeyFactory rsaKeyFactory() {
            return new RSAKeyFactory();
        }

        @Bean
        public Algorithm jwtPrivateKeyAlgorithm(
                @Value("${auth.jwt.crypto.private-key}") String privateKeyString,
                RSAKeyFactory rsaKeyFactory
        ) throws InvalidKeySpecException {
            RSAPrivateKey privateKey = rsaKeyFactory.createPrivateKey(privateKeyString);
            return Algorithm.RSA256(privateKey);
        }

        @Bean
        public Algorithm jwtPublicKeyAlgorithm(
                @Value("${auth.jwt.crypto.public-key}") String publicKeyString,
                RSAKeyFactory rsaKeyFactory
        ) throws InvalidKeySpecException {
            RSAPublicKey publicKey = rsaKeyFactory.createPublicKey(publicKeyString);
            return Algorithm.RSA256(publicKey);
        }
    }

    public static final MemberPrincipal VALID_MEMBER_PRINCIPAL = new MemberPrincipal(1L, Role.MEMBER);
    public static final MemberPrincipal VALID_ADMIN_PRINCIPAL = new MemberPrincipal(2L, Role.ADMIN);
    public static final MemberPrincipal VALID_GUEST_PRINCIPAL = new MemberPrincipal(3L, Role.GUEST);
    public static final String VALID_MEMBER_JWT = "token.validate.member";
    public static final String VALID_ADMIN_JWT = "token.validate.admin";
    public static final String VALID_GUEST_JWT = "token.validate.guest";
}

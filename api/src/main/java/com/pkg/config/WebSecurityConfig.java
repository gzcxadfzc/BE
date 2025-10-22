package com.pkg.config;

import com.pkg.authentication.token.AccessTokenAuthenticator;
import com.pkg.support.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

@Configuration
public class WebSecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver,
            AccessTokenAuthenticator tokenAuthenticator,
            @Qualifier("shouldNotFilterUri") List<String> shouldNotFilterUri
    ) {
        return new JwtAuthenticationFilter(
                shouldNotFilterUri,
                tokenAuthenticator,
                resolver
        );
    }

    @Bean
    public List<String> shouldNotFilterUri() {
        return List.of(
                "/api/v1/health",
                "/api/v1/book/board/**",
                "/api/v1/auth/**"
        );
    }
}

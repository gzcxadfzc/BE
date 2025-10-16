package com.pkg.openai.api;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenAiFeignConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenAiFeignConfig.class);

    @Value("${ai.open-ai.api.service-key}")
    private String apiKey;

    @Bean
    public RequestInterceptor openAiRequestInterceptor() {
        return template -> {
            template.header("Authorization", "Bearer " + apiKey);
            template.header("Content-Type", "application/json");
        };
    }

    @Bean
    public ErrorDecoder customErrorDecoder() {
        return new OpenAiErrorDecoder();
    }
}

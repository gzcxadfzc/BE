package com.pkg.openai;

import com.pkg.openai.api.OpenAiErrorDecoder;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableFeignClients
@Configuration
public class AiFeignConfig {

    private static final Logger log = LoggerFactory.getLogger(AiFeignConfig.class);

    @Bean
    public RequestInterceptor loggingRequestInterceptor() {
        return template -> {
            log.info("[Feign Request] {} {}", template.method(), template.url());
            log.info("Headers: {}", template.headers());
            if (template.body() != null) {
                log.info("Body: {}", new String(template.body()));
            }
        };
    }

    @Bean
    public ErrorDecoder openAiErrorDecoder() {
        return new OpenAiErrorDecoder();
    }
}

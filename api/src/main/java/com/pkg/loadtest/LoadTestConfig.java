package com.pkg.loadtest;

import com.pkg.domain.ai.BookCharacterGenerator;
import com.pkg.domain.image.ImageRepository;
import com.pkg.domain.image.ImageUploadResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("load-test")
public class LoadTestConfig {

    private static final String FAKE_IMAGE_URL = "https://fake.image/load-test.png";

    @Bean
    @Primary
    public BookCharacterGenerator noOpBookCharacterGenerator() {
        return request -> FAKE_IMAGE_URL;
    }

    @Bean
    @Primary
    public ImageRepository noOpImageRepository() {
        return new ImageRepository() {
            @Override
            public ImageUploadResult uploadTemporary(String url) {
                return new ImageUploadResult(url, FAKE_IMAGE_URL);
            }

            @Override
            public ImageUploadResult uploadCharacterImage(String url) {
                return new ImageUploadResult(url, FAKE_IMAGE_URL);
            }
        };
    }
}

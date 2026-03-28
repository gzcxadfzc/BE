package com.pkg.loadtest;

import com.pkg.domain.ai.BookPageGenerated;
import com.pkg.domain.ai.BookPageGenerator;
import com.pkg.domain.image.ImageRepository;
import com.pkg.domain.image.ImageUploadResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@Profile("load-test")
public class LoadTestConfig {

    private static final String FAKE_IMAGE_URL = "https://fake.image/load-test.png";

    @Bean
    @Primary
    public BookPageGenerator noOpBookPageGenerator() {
        return bookToProgress -> new BookPageGenerated(
                FAKE_IMAGE_URL,
                "부하 테스트용 더미 컨텍스트입니다.",
                List.of("질문1", "질문2", "질문3")
        );
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

            @Override
            public Map<String, ImageUploadResult> copyAllToPermanentStorage(List<String> urls) {
                return urls.stream()
                        .collect(Collectors.toMap(u -> u, u -> new ImageUploadResult(u, FAKE_IMAGE_URL)));
            }
        };
    }
}

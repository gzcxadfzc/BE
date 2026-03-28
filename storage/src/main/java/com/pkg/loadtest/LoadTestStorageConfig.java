package com.pkg.loadtest;

import com.pkg.domain.image.ImageUploadResult;
import com.pkg.s3.AsyncBucketImageUploader;
import com.pkg.s3.PreAssignedUrl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.CompletableFuture;

@Configuration
@Profile("load-test")
public class LoadTestStorageConfig {

    private static final String FAKE_IMAGE_URL = "https://fake.image/load-test.png";

    @Bean
    @Primary
    public AsyncBucketImageUploader noOpAsyncBucketImageUploader() {
        return new AsyncBucketImageUploader(null) {
            @Override
            public CompletableFuture<ImageUploadResult> copyToBookStorage(String url) {
                return CompletableFuture.completedFuture(new ImageUploadResult(url, FAKE_IMAGE_URL));
            }

            @Override
            public void copyToBookStorage(PreAssignedUrl url) {
                // no-op
            }
        };
    }
}

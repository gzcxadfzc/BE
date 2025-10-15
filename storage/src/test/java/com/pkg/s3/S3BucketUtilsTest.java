package com.pkg.s3;

import com.pkg.config.AwsS3Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
class S3BucketUtilsTest {

    @Autowired
    private S3BucketUtils s3BucketUtils;

    private static final String TEST_KEY = "test/image.png";
    private static final String TEST_COPY_KEY = "test/copy/image.png";

    private InputStream inputStream;
    private long length;

    @BeforeEach
    void setUp() throws IOException {
        ClassPathResource resource = new ClassPathResource("images/test_image.png");
        inputStream = resource.getInputStream();
        length = resource.contentLength();
    }

    @Test
    @DisplayName("png파일을 S3에 업로드 및 삭제")
    void testUploadMultipartFile() throws IOException {
        // When
        s3BucketUtils.uploadPng(inputStream, length, TEST_KEY);

        // Then
        GetObjectResponse response = s3BucketUtils.get(TEST_KEY);
        assertThat(response).isNotNull();
        assertThat(response.metadata())
                .contains(Map.entry("uploader", "little-writer"));

        s3BucketUtils.delete(TEST_KEY);
        assertThatThrownBy(() -> s3BucketUtils.get(TEST_KEY))
                .isInstanceOf(NoSuchKeyException.class);
    }

    @Test
    @DisplayName("S3 내에서 파일 복사")
    void testCopyFile() {
        // Given
        s3BucketUtils.uploadPng(inputStream, length, TEST_KEY);

        // When
        s3BucketUtils.copyFile(TEST_KEY, TEST_COPY_KEY);

        // Then
        GetObjectResponse response = s3BucketUtils.get(TEST_COPY_KEY);
        assertThat(response).isNotNull();
        assertThat(response.metadata())
                .contains(Map.entry("uploader", "little-writer"));

        s3BucketUtils.delete(TEST_COPY_KEY);
    }

    @Test
    @DisplayName("url 업로드 테스트")
    void testUploadFromUrl() throws IOException, URISyntaxException {
        // Given
        String url = "https://upload.wikimedia.org/wikipedia/commons/7/70/Example.png";
        // When
        s3BucketUtils.uploadFromUrl(url, TEST_KEY);

        // Then
        GetObjectResponse response = s3BucketUtils.get(TEST_KEY);
        assertThat(response).isNotNull();
        assertThat(response.metadata())
                .contains(Map.entry("uploader", "little-writer"));

        s3BucketUtils.delete(TEST_KEY);
    }
}

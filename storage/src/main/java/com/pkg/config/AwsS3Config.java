package com.pkg.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
@EnableConfigurationProperties(AwsS3Config.S3Properties.class)
public class AwsS3Config {

    @Bean
    public S3Client s3Client(S3Properties properties) {
        AwsCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));

        S3Configuration s3config = S3Configuration.builder()
                .build();

        return  S3Client.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentials)
                .serviceConfiguration(s3config)
                .build();
    }

    @ConfigurationProperties(prefix = "storage.s3")
    public record S3Properties(
            String bucket,
            String region,
            String accessKey,
            String secretKey,
            String cdn
    ) {}
}

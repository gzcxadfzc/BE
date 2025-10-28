package com.pkg.s3;

import com.pkg.config.AwsS3Config;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

@Component
public class S3BucketUtils {

    private static final Map<String, String> META = Map.of(
            "uploader", "little-writer"
    );
    private final S3Client s3Client;
    private final AwsS3Config.S3Properties properties;

    public S3BucketUtils(S3Client amazonS3Client, AwsS3Config.S3Properties properties) {
        this.s3Client = amazonS3Client;
        this.properties = properties;
    }

    public PutObjectResponse uploadPng(InputStream inputStream, long size, String uploadName) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .contentType("image/png")
                .contentLength(size)
                .key(uploadName)
                .metadata(META)
                .build();

        return s3Client.putObject(request, RequestBody.fromInputStream(inputStream, size));
    }


    public DeleteObjectResponse delete(String fileKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(fileKey)
                .build();
        return s3Client.deleteObject(request);
    }

    public PutObjectResponse uploadFromUrl(String fromurl, String fileKey) throws IOException, URISyntaxException {

        URL url = new URI(fromurl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "LittleWriterBot/1.0");
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestMethod("GET");

        // ContentType, ContentLength 추출
        String contentType = conn.getContentType();
        long contentLength = conn.getContentLengthLong();

        try (InputStream inputStream = conn.getInputStream()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(fileKey)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .metadata(META)
                    .build();
            return s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
        }
    }

    public CopyObjectResponse copyFile(String sourceKey, String destinationKey) {
        CopyObjectRequest request = CopyObjectRequest.builder()
                .sourceBucket(properties.bucket())
                .sourceKey(sourceKey)
                .destinationKey(destinationKey)
                .destinationBucket(properties.bucket())
                .metadataDirective("COPY")
                .build();
        return s3Client.copyObject(request);
    }

    public GetObjectResponse get(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .build();
        return s3Client.getObject(request).response();
    }
}

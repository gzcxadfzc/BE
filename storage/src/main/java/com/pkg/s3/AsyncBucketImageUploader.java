package com.pkg.s3;

import com.pkg.domain.image.ImageException;
import com.pkg.domain.image.ImageUploadResult;
import com.pkg.domain.uitl.UuidGen;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;

@Component
public class AsyncBucketImageUploader {

    private final S3BucketUtils s3BucketUtils;

    public AsyncBucketImageUploader(S3BucketUtils s3BucketUtils) {
        this.s3BucketUtils = s3BucketUtils;
    }

    private static final String BUCKET_HOST = "https://littlewriter.s3.ap-northeast-2.amazonaws.com/";

    @Async("s3-bucket")
    public CompletableFuture<ImageUploadResult> copyToBookStorage(String url) {
        if(url.startsWith(BUCKET_HOST)) {
            String sourceKey = url.replace(BUCKET_HOST, "");
            String destinationKey = UuidGen.prefixed("book/") + ".png";
            CopyObjectResponse response = s3BucketUtils.copyFile(sourceKey, destinationKey);
            return CompletableFuture.completedFuture(new ImageUploadResult(url, BUCKET_HOST + destinationKey));
        }

        try {
            String destinationKey = UuidGen.prefixed("book/") + ".png";
            s3BucketUtils.uploadFromUrl(url, destinationKey);
            return CompletableFuture.completedFuture(new ImageUploadResult(url, BUCKET_HOST + destinationKey));
        } catch (IOException | URISyntaxException e ) {
            throw ImageException.uploadFailed(e.getMessage());
        }
    }
}

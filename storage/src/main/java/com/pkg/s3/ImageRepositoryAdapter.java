package com.pkg.s3;

import com.pkg.domain.image.ImageException;
import com.pkg.domain.image.ImageRepository;
import com.pkg.domain.image.ImageUploadResult;
import com.pkg.domain.uitl.UuidGen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class ImageRepositoryAdapter implements ImageRepository {

    private static final Logger log = LoggerFactory.getLogger(ImageRepositoryAdapter.class);
    private static final String BUCKET_HOST = "https://littlewriter.s3.ap-northeast-2.amazonaws.com/";
    private final S3BucketUtils s3BucketUtils;
    private final AsyncBucketImageUploader asyncBucketImageUploader;


    public ImageRepositoryAdapter(S3BucketUtils s3BucketUtils, AsyncBucketImageUploader asyncBucketImageUploader) {
        this.s3BucketUtils = s3BucketUtils;
        this.asyncBucketImageUploader = asyncBucketImageUploader;
    }

    @Override
    public ImageUploadResult uploadTemporary(String url) {
        try {
            String destinationKey = UuidGen.prefixed(S3KeyPrefix.TEMPORARY.getPrefix()) + ".png";
            if(destinationKey.startsWith("/")) {
                destinationKey = destinationKey.substring(1);
            }
            s3BucketUtils.uploadFromUrl(url, destinationKey);
            return new ImageUploadResult(url, BUCKET_HOST + destinationKey);
        } catch (IOException | URISyntaxException | SdkException e ) {
            throw ImageException.uploadFailed(e.getMessage());
        }
    }

    @Override
    public ImageUploadResult uploadCharacterImage(String url) {
        try {
            String destinationKey = UuidGen.prefixed(S3KeyPrefix.CHARACTER.getPrefix()) + ".png";
            s3BucketUtils.uploadFromUrl(url, BUCKET_HOST + destinationKey);
            return new ImageUploadResult(url, BUCKET_HOST + destinationKey);
        } catch (IOException | URISyntaxException | SdkException e) {
            throw ImageException.uploadFailed(e.getMessage());
        }
    }

    @Override
    public Map<String, ImageUploadResult> copyAllToPermanentStorage(List<String> urls) {
        List<CompletableFuture<ImageUploadResult>> futures =
                urls.stream()
                        .map(asyncBucketImageUploader::copyToBookStorage)
                        .toList();
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("Some uploads failed", e.getCause());
            throw ImageException.uploadFailed(e.getCause().getMessage());
        }

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toMap(ImageUploadResult::originUrl, r -> r));
    }

}

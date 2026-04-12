package com.pkg.s3;

import com.pkg.domain.image.ImageException;
import com.pkg.domain.image.ImageRepository;
import com.pkg.domain.image.ImageUploadResult;
import com.pkg.domain.uitl.UuidGen;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;

import java.io.IOException;
import java.net.URISyntaxException;

@Component
public class ImageRepositoryAdapter implements ImageRepository {

    private static final String BUCKET_HOST = "https://littlewriter.s3.ap-northeast-2.amazonaws.com/";
    private final S3BucketUtils s3BucketUtils;

    public ImageRepositoryAdapter(S3BucketUtils s3BucketUtils) {
        this.s3BucketUtils = s3BucketUtils;
    }

    @Override
    public ImageUploadResult uploadTemporary(String url) {
        try {
            String destinationKey = UuidGen.prefixed(S3KeyPrefix.TEMPORARY.getPrefix()) + ".png";
            if (destinationKey.startsWith("/")) {
                destinationKey = destinationKey.substring(1);
            }
            s3BucketUtils.uploadFromUrl(url, destinationKey);
            return new ImageUploadResult(url, BUCKET_HOST + destinationKey);
        } catch (IOException | URISyntaxException | SdkException e) {
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
}

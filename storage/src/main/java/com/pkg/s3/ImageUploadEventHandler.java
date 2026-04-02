package com.pkg.s3;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ImageUploadEventHandler {

    private final AsyncBucketImageUploader imageUploader;

    public ImageUploadEventHandler(AsyncBucketImageUploader imageUploader) {
        this.imageUploader = imageUploader;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async(value = "transaction-event")
    public void handle(ImageUploadEvent event) {
        for (PreAssignedUrl url : event.jobs()) {
            imageUploader.copyToBookStorage(url);
        }
    }
}

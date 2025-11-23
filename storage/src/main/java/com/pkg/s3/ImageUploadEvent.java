package com.pkg.s3;

import java.util.List;

public record ImageUploadEvent(
        List<PreAssignedUrl> jobs
) {
}

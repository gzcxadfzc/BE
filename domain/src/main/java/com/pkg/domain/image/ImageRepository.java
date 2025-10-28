package com.pkg.domain.image;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public interface ImageRepository {

    ImageUploadResult uploadTemporary(String url);

    Map<String, ImageUploadResult> copyAllToPermanentStorage(List<String> urls);
}

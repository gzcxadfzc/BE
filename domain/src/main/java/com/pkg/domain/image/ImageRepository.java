package com.pkg.domain.image;

import org.springframework.stereotype.Component;

@Component
public interface ImageRepository {
    ImageUploadResult uploadTemporary(String url);
    ImageUploadResult uploadCharacterImage(String url);
}

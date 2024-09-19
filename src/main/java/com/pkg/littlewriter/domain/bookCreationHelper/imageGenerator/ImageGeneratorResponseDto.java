package com.pkg.littlewriter.domain.bookCreationHelper.imageGenerator;

import lombok.Getter;

@Getter
public class ImageGeneratorResponseDto {
    private final String imageUrl;

    public ImageGeneratorResponseDto (String imageUrl) {
        this.imageUrl = imageUrl;
    }
}

package com.pkg.littlewriter.domain.bookCreationHelper.imageGenerator;

import lombok.Getter;

@Getter
public class ImageGeneratorInputDto {
    private final String context;

    public ImageGeneratorInputDto(String context) {
        this.context = context;
    }
}

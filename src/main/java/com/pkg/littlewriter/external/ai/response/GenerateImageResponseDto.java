package com.pkg.littlewriter.external.ai.response;

import lombok.Getter;

@Getter
public class GenerateImageResponseDto {
    private final String imageUrl;

    public GenerateImageResponseDto(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}

package com.pkg.core.response;

import lombok.Getter;

@Getter
public class GenerateImageResponseDto {
    private final String imageUrl;

    public GenerateImageResponseDto(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}

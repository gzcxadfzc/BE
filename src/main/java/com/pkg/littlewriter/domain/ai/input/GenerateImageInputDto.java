package com.pkg.littlewriter.domain.ai.input;

import lombok.Getter;

@Getter
public class GenerateImageInputDto {
    private final String context;

    public GenerateImageInputDto(String context) {
        this.context = context;
    }
}

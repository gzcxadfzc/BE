package com.pkg.core.input;

import lombok.Getter;

@Getter
public class GenerateImageInputDto {
    private final String context;

    public GenerateImageInputDto(String context) {
        this.context = context;
    }
}

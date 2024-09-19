package com.pkg.littlewriter.domain.external.ai.response;

import lombok.Getter;

import java.util.List;

@Getter
public class GenerateContextQuestionResponseDto {
    private final String refinedText;
    private final List<String> questions;

    public GenerateContextQuestionResponseDto(String refinedText, List<String> questions) {
        this.refinedText = refinedText;
        this.questions = questions;
    }
}

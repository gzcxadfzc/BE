package com.pkg.littlewriter.domain.ai.response;

import lombok.Getter;

import java.util.List;

@Getter
public class ContextAndQuestionDto {
    private final String refinedText;
    private final List<String> questions;

    public ContextAndQuestionDto(String refinedText, List<String> questions) {
        this.refinedText = refinedText;
        this.questions = questions;
    }
}

package com.pkg.littlewriter.domain.external.ai.input;

import lombok.Builder;

@Builder
public class GenerateContextQuestionInputDto {
    private final String previousContext;
    private final String currentContext;
    private final String mainCharacterName;
    private final String personality;
}

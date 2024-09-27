package com.pkg.littlewriter.external.ai.input;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateContextQuestionInputDto {
    private String previousContext;
    private String currentContext;
    private String mainCharacterName;
    private String personality;
}

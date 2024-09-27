package com.pkg.littlewriter.external.ai.response;

import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateContextQuestionResponseDto {
    private String refinedText;
    private List<String> questions;
}

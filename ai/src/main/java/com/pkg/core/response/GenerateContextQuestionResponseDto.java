package com.pkg.core.response;

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

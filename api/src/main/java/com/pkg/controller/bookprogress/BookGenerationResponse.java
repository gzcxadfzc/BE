package com.pkg.controller.bookprogress;

import com.pkg.domain.bookprogress.AiGenerateResult;

import java.util.List;

public record BookGenerationResponse(
        BookInProgressResponse bookInProgress,
        List<String> questions
) {

    public static BookGenerationResponse from(AiGenerateResult result) {
        return new BookGenerationResponse(
                BookInProgressResponse.from(result),
                result.questions()
        );
    }
}

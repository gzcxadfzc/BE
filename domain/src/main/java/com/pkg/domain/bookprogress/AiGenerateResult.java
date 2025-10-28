package com.pkg.domain.bookprogress;

import java.util.List;

public record AiGenerateResult(
        BookInProgress bookInProgress,
        List<String> questions
) {
}

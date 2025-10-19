package com.pkg.domain.ai;

import com.pkg.domain.bookprogress.BookInProgress;

public record CreateOnePageRequest(
        BookInProgress bookInProgress,
        String userInput
) {
}

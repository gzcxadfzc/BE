package com.pkg.domain.book;

public record CreateOnePageRequest(
        String bookInProgressId,
        String userInput
) {
}

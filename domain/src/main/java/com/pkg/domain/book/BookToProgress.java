package com.pkg.domain.book;

public record BookToProgress(
    BookInProgress bookInProgress,
    String userInput
) {
}

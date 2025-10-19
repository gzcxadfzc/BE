package com.pkg.domain.bookprogress;

public record BookToProgress(
    BookInProgress bookInProgress,
    String userInput
) {
}

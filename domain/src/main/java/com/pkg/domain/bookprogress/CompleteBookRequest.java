package com.pkg.domain.bookprogress;

public record CompleteBookRequest(
        String memberId,
        BookInProgress bookInProgress,
        String title,
        String author
) {
}

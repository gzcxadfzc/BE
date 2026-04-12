package com.pkg.domain.bookprogress;

public record BookPagePollResult(
        String status,
        BookPageResult page
) {
    public static BookPagePollResult pending() {
        return new BookPagePollResult("PENDING", null);
    }

    public static BookPagePollResult completed(BookPageResult page) {
        return new BookPagePollResult("COMPLETED", page);
    }
}

package com.pkg.domain.book;

import java.util.ArrayList;
import java.util.List;

public record BookInProgress(
    String id,
    String bookTitle,
    Character character,
    List<BookPage> previousPages
) {
    public BookInProgress addBookPage(BookPage bookPage) {
        List<BookPage> updated = new ArrayList<>(previousPages);
        updated.add(bookPage);
        return new BookInProgress(
                this.id,
                this.bookTitle,
                this.character,
                updated
        );
    }
}

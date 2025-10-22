package com.pkg.domain.bookprogress;

import com.pkg.domain.book.BookPage;
import com.pkg.domain.character.BookCharacter;

import java.util.ArrayList;
import java.util.List;

public record BookInProgress(
    String id,
    Long ownerId,
    String backgroundInfo,
    BookCharacter character,
    List<BookPage> previousPages
) {
    public BookInProgress addBookPage(BookPage bookPage) {
        List<BookPage> updated = new ArrayList<>(previousPages);
        updated.add(bookPage);
        return new BookInProgress(
                this.id,
                this.ownerId,
                this.backgroundInfo,
                this.character,
                updated
        );
    }
}

package com.pkg.controller.book;

import com.pkg.domain.book.Book;

import java.util.List;

public record BookResponse(
        String id,
        Long memberId,
        List<Page> bookPages,
        String title,
        String author,
        CurrentCharacter character
) {
    record CurrentCharacter(
            Long id,
            String name,
            String personality,
            String description,
            String imageUrl
    ) {
    }

    record Page(
            String context,
            String imageUrl,
            int pageNumber
    ) {
    }

    public static BookResponse from(Book book) {
        return new BookResponse(
                book.id(),
                book.memberId(),
                book.bookPages()
                        .stream()
                        .map(bookpage -> new Page(bookpage.context(), bookpage.imageUrl(), bookpage.pageNumber()))
                        .toList(),
                book.title(),
                book.author(),
                new CurrentCharacter(
                        book.character().id(),
                        book.character().name(),
                        book.character().personality(),
                        book.character().description(),
                        book.character().imageUrl()
                )
        );
    }
}

package com.pkg.domain.book;

public interface BookPageGenerator {

    BookInProgress initBook(BookInitRequest bookInit);

    BookPage createOnePage(BookToProgress bookToProgress);
}

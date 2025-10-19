package com.pkg.domain.ai;

import com.pkg.domain.book.BookPage;
import com.pkg.domain.bookprogress.BookInProgressRepository;
import com.pkg.domain.bookprogress.BookToProgress;

public class BookPageService {

    private final BookPageGenerator bookPageGenerator;

    public BookPageService(
            BookPageGenerator bookPageGenerator,
            BookInProgressRepository bookInProgressRepository) {
        this.bookPageGenerator = bookPageGenerator;
    }

    public BookPage create(CreateOnePageRequest request) {
        BookToProgress bookToProgress = new BookToProgress(request.bookInProgress(), request.userInput());
        return bookPageGenerator.generatePageFrom(bookToProgress);
    }
}

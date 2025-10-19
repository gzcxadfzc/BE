package com.pkg.domain.ai;

import com.pkg.domain.bookprogress.BookInProgress;
import com.pkg.domain.book.BookInitRequest;
import com.pkg.domain.book.BookPage;
import com.pkg.domain.bookprogress.BookToProgress;

public interface BookPageGenerator {

    BookInProgress initBook(BookInitRequest bookInit);

    BookPage generatePageFrom(BookToProgress bookToProgress);
}

package com.pkg.domain.book;

import com.pkg.domain.bookprogress.BookInProgress;
import com.pkg.domain.common.PageResult;
import com.pkg.domain.member.Actor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.function.Function;

@Repository
public interface BookRepository {

    Book saveFrom(BookInProgress bookInProgress, Function<BookInProgress, Book> converter);

    Book save(Book book);

    Book retrieveById(String bookId);

    List<BookThumbnail> retrieveThumbnailsByUser(Actor currentUser);

    PageResult<BookThumbnail> retrieveThumbnails(BookRetrieveQuery bookRetrieveQuery);
}

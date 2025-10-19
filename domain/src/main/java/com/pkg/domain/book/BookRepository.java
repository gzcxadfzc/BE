package com.pkg.domain.book;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository {

    void save(Book book);

    Book retrieveById(String bookId);

    List<Book> retrieveByMemberId(long userId);

    List<BookThumbnail> retrieveThumbnailsByMemberId(long userId);
}

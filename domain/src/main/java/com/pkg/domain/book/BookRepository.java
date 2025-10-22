package com.pkg.domain.book;

import com.pkg.domain.member.Actor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository {

    Book save(Book book);

    Book retrieveById(String bookId);

    List<BookThumbnail> retrieveThumbnailsByUser(Actor currentUser);
}

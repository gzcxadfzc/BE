package com.pkg.domain.bookprogress;

import com.pkg.domain.book.BookPage;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookInProgressRepository {

    List<BookInProgress> retrieveByMemberId(Long memberId);

    BookInProgress retrieveById(String id);

    BookInProgress save(BookInProgress bookInProgress);

    BookInProgress addPageTo(String id, BookPage bookPage);
}

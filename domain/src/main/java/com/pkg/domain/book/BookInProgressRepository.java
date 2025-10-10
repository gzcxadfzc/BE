package com.pkg.domain.book;

import org.springframework.stereotype.Repository;

@Repository
public interface BookInProgressRepository {

    BookInProgress retrieveById(String id);

    void addPage(String id, BookPage bookPage);

    void save(BookInProgress bookInProgress);
}

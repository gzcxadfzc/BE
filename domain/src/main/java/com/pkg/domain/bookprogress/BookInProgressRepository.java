package com.pkg.domain.bookprogress;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookInProgressRepository {

    List<BookInProgress> retrieveByMemberId(String memberId);

    BookInProgress retrieveById(String id);

    void save(BookInProgress bookInProgress);
}

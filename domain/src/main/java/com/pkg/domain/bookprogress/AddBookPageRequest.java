package com.pkg.domain.bookprogress;

import com.pkg.domain.book.BookPage;
import com.pkg.domain.member.Actor;

public record AddBookPageRequest(
        Actor currentUser,
        BookInProgress bookInProgress,
        BookPage bookPage
) {
}

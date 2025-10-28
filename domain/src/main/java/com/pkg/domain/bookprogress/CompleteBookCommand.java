package com.pkg.domain.bookprogress;

import com.pkg.domain.member.Actor;

public record CompleteBookCommand(
        Actor actor,
        String bookInProgressId,
        String title,
        String author
) {
}

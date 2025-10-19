package com.pkg.domain.book;

import com.pkg.domain.member.Actor;

public record BookInitRequest(
        Character character,
        String background,
        Actor currentUser
) {
}

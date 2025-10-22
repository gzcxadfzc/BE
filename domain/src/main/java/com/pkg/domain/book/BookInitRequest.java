package com.pkg.domain.book;

import com.pkg.domain.character.BookCharacter;
import com.pkg.domain.member.Actor;

public record BookInitRequest(
        BookCharacter character,
        String background,
        Actor currentUser
) {
}
